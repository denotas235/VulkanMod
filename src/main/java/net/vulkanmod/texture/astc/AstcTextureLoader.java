package net.vulkanmod.texture.astc;

import net.vulkanmod.Initializer;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Carregador de texturas ASTC pré-compiladas
 * 
 * Funcionamento real (SEM decompressão CPU):
 * 1. Lê blocos ASTC raw diretamente do arquivo .astc
 * 2. Retorna ByteBuffer com dados comprimidos
 * 3. VulkanImage.uploadAstcCompressed() faz upload direto para GPU
 * 4. GPU Mali-G52 decompõe ASTC hardware on-the-fly durante leitura
 * 
 * Formato do arquivo .astc:
 * - Raw blocks ASTC (sem header) - cada bloco = 16 bytes
 * - Block size inferido do path ou metadados
 */
public class AstcTextureLoader {
    
    private static final boolean DEBUG = true;
    
    /**
     * Carrega textura ASTC de um resource path
     * 
     * @param resourcePath path do recurso (ex: "textures/block/dirt.png")
     * @return AstcTextureData com blocos ASTC raw + metadados, ou null se não existir
     */
    public static AstcTextureData loadAstcTexture(String resourcePath) {
        String astcPath = "/assets/minecraft/" + resourcePath.replace(".png", ".astc");
        
        try (InputStream astcStream = AstcTextureLoader.class.getResourceAsStream(astcPath)) {
            if (astcStream == null) {
                if (DEBUG) {
                    Initializer.LOGGER.debug("[ASTC] Não encontrado: {}", astcPath);
                }
                return null;
            }
            
            byte[] astcBytes = astcStream.readAllBytes();
            if (astcBytes.length < 16) {
                Initializer.LOGGER.error("[ASTC] Arquivo corrompido (tamanho < 16 bytes): {}", astcPath);
                return null;
            }
            
            // Verificar magic number ASTC (0x5CA1AB13 em little-endian: 13 ab a1 5c)
            if (astcBytes[0] != 0x13 || astcBytes[1] != (byte)0xAB || astcBytes[2] != (byte)0xA1 || astcBytes[3] != 0x5C) {
                Initializer.LOGGER.error("[ASTC] Magic number inválido no arquivo: {}", astcPath);
                return null;
            }
            
            // Ler block dims
            int blockSizeX = astcBytes[4] & 0xFF;
            int blockSizeY = astcBytes[5] & 0xFF;
            
            // Ler dimensões da imagem (valores de 24 bits em little-endian)
            int width = (astcBytes[7] & 0xFF) | ((astcBytes[8] & 0xFF) << 8) | ((astcBytes[9] & 0xFF) << 16);
            int height = (astcBytes[10] & 0xFF) | ((astcBytes[11] & 0xFF) << 8) | ((astcBytes[12] & 0xFF) << 16);
            
            // Copiar apenas os dados comprimidos raw (pulando o cabeçalho de 16 bytes)
            int dataSize = astcBytes.length - 16;
            ByteBuffer astcBuffer = MemoryUtil.memAlloc(dataSize);
            astcBuffer.put(astcBytes, 16, dataSize);
            astcBuffer.flip();
            
            boolean srgb = inferDimensions(resourcePath).srgb;
            
            AstcTextureData data = new AstcTextureData(
                astcBuffer,
                width,
                height,
                blockSizeX,
                srgb
            );
            
            if (DEBUG) {
                Initializer.LOGGER.debug(
                    "[ASTC] Carregada: {} → {}x{} (bloco {}x{}, {} bytes comprimidos, sRGB={})",
                    resourcePath,
                    width,
                    height,
                    blockSizeX,
                    blockSizeY,
                    dataSize,
                    srgb
                );
            }
            
            return data;
            
        } catch (IOException e) {
            Initializer.LOGGER.error("[ASTC] Erro ao carregar: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * Infer dimensões da textura baseado no path
     */
    private static TextureDimensions inferDimensions(String resourcePath) {
        // Blocks e items padrão: 16x16
        if (resourcePath.contains("/block/") || resourcePath.contains("/item/")) {
            // Exceções para texturas animadas
            if (resourcePath.contains("fire_") || resourcePath.contains("water_") || 
                resourcePath.contains("lava_") || resourcePath.contains("portal")) {
                // Animações são mais altas
                return new TextureDimensions(16, 512, true);
            }
            return new TextureDimensions(16, 16, false);
        }
        
        // GUI varia muito
        if (resourcePath.contains("/gui/")) {
            if (resourcePath.contains("title/")) {
                if (resourcePath.contains("minecraft") || resourcePath.contains("minceraft")) {
                    return new TextureDimensions(1024, 256, false);
                }
                if (resourcePath.contains("realms")) {
                    return new TextureDimensions(256, 256, false);
                }
                return new TextureDimensions(512, 256, false);
            }
            if (resourcePath.contains("container/") || resourcePath.contains("sprites/")) {
                return new TextureDimensions(256, 256, false);
            }
            return new TextureDimensions(256, 256, false);
        }
        
        // Entities variam
        if (resourcePath.contains("/entity/")) {
            if (resourcePath.contains("enderdragon")) {
                return new TextureDimensions(256, 256, false);
            }
            if (resourcePath.contains("conduit")) {
                return new TextureDimensions(64, 704, false);
            }
            return new TextureDimensions(64, 64, false);
        }
        
        // Environment
        if (resourcePath.contains("/environment/")) {
            if (resourcePath.contains("sky")) {
                return new TextureDimensions(128, 128, false);
            }
            if (resourcePath.contains("clouds")) {
                return new TextureDimensions(256, 256, false);
            }
            return new TextureDimensions(256, 256, false);
        }
        
        // Paintings
        if (resourcePath.contains("/painting/")) {
            return new TextureDimensions(64, 64, false);
        }
        
        // Particles
        if (resourcePath.contains("/particle/")) {
            return new TextureDimensions(32, 32, true);
        }
        
        // Colormaps
        if (resourcePath.contains("/colormap/")) {
            return new TextureDimensions(256, 256, false);
        }
        
        // Default: 16x16
        return new TextureDimensions(16, 16, false);
    }
    
    /**
     * Infer block size ASTC baseado no tamanho do arquivo e dimensões
     */
    private static int inferBlockSize(int astcSize, int width, int height) {
        // ASTC: cada bloco = 16 bytes
        int totalBlocks = astcSize / 16;
        
        // Testar block sizes comuns
        int[] blockSizes = {4, 5, 6, 8, 10, 12};
        
        for (int bs : blockSizes) {
            int blocksX = (width + bs - 1) / bs;
            int blocksY = (height + bs - 1) / bs;
            int expectedBlocks = blocksX * blocksY;
            
            if (expectedBlocks == totalBlocks) {
                return bs;
            }
            
            // Margem de erro de 1 block (padding)
            if (Math.abs(expectedBlocks - totalBlocks) <= 1) {
                return bs;
            }
        }
        
        // Default: 6x6 (bom balanço qualidade/tamanho para maioria)
        Initializer.LOGGER.warn(
            "[ASTC] Block size não detectado para {}x{} ({} bytes), usando 6x6",
            width, height, astcSize
        );
        return 6;
    }
    
    /**
     * Dados de textura ASTC carregada
     */
    public static class AstcTextureData {
        public final ByteBuffer astcData;      // Blocos ASTC raw
        public final int width;
        public final int height;
        public final int blockSize;
        public final boolean srgb;
        public final int vkFormat;
        
        public AstcTextureData(ByteBuffer astcData, int width, int height, int blockSize, boolean srgb) {
            this.astcData = astcData;
            this.width = width;
            this.height = height;
            this.blockSize = blockSize;
            this.srgb = srgb;
            this.vkFormat = calculateVkFormat(blockSize, srgb);
        }
        
        /**
         * Calcula VkFormat ASTC
         */
        private int calculateVkFormat(int blockSize, boolean srgb) {
            return switch (blockSize) {
                case 4 -> srgb ? 0x93B1 : 0x93B0; // VK_FORMAT_ASTC_4x4_SRGB_BLOCK / UNORM
                case 5 -> srgb ? 0x93B3 : 0x93B2; // VK_FORMAT_ASTC_5x5_SRGB_BLOCK / UNORM
                case 6 -> srgb ? 0x93B5 : 0x93B4; // VK_FORMAT_ASTC_6x6_SRGB_BLOCK / UNORM
                case 8 -> srgb ? 0x93B7 : 0x93B6; // VK_FORMAT_ASTC_8x8_SRGB_BLOCK / UNORM
                case 10 -> srgb ? 0x93B9 : 0x93B8; // VK_FORMAT_ASTC_10x10_SRGB_BLOCK / UNORM
                case 12 -> srgb ? 0x93BB : 0x93BA; // VK_FORMAT_ASTC_12x12_SRGB_BLOCK / UNORM
                default -> 0x93B4; // Default: 6x6 UNORM
            };
        }
        
        /**
         * Libera memória alocada
         */
        public void close() {
            if (astcData != null) {
                MemoryUtil.memFree(astcData);
            }
        }
    }
    
    /**
     * Dimensões de textura inferidas
     */
    private static class TextureDimensions {
        final int width;
        final int height;
        final boolean srgb;
        
        TextureDimensions(int width, int height, boolean srgb) {
            this.width = width;
            this.height = height;
            this.srgb = srgb;
        }
    }
}