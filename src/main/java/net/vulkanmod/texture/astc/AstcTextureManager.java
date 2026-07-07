package net.vulkanmod.texture.astc;

import net.vulkanmod.Initializer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador de cache de texturas ASTC
 * Singleton que mantém texturas ASTC carregadas em cache
 */
public class AstcTextureManager {
    private static final AstcTextureManager INSTANCE = new AstcTextureManager();
    
    // Cache LRU simples
    private final Map<String, AstcTextureLoader.AstcTextureData> textureCache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 256;
    
    private boolean initialized = false;
    
    private AstcTextureManager() {}
    
    public static AstcTextureManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Inicializa o sistema ASTC
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        // GPU Mali-G52 suporta ASTC LDR nativamente
        // Não precisa verificar extensões Vulkan explicitamente
        this.initialized = true;
        
        Initializer.LOGGER.info("[ASTC] Suporte ASTC inicializado (upload direto GPU)");
        Initializer.LOGGER.info("[ASTC] Cache size: {} texturas máx", MAX_CACHE_SIZE);
        Initializer.LOGGER.info("[ASTC] Decompressão: Hardware (GPU Mali-G52)");
    }
    
    /**
     * Carrega textura ASTC com cache
     * 
     * @param resourcePath path do recurso (ex: "textures/block/dirt.png")
     * @return AstcTextureData ou null se não existir ASTC
     */
    @Nullable
    public synchronized AstcTextureLoader.AstcTextureData loadTexture(String resourcePath) {
        if (!initialized) {
            return null;
        }
        
        // Verificar cache
        AstcTextureLoader.AstcTextureData cached = textureCache.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        
        // Carregar ASTC
        AstcTextureLoader.AstcTextureData data = AstcTextureLoader.loadAstcTexture(resourcePath);
        if (data == null) {
            return null;
        }
        
        // Adicionar ao cache (evitar memory leak)
        if (textureCache.size() >= MAX_CACHE_SIZE) {
            // Remover primeira entrada (LRU simples)
            String firstKey = textureCache.keySet().iterator().next();
            AstcTextureLoader.AstcTextureData toRemove = textureCache.remove(firstKey);
            if (toRemove != null) {
                toRemove.close();
            }
        }
        
        textureCache.put(resourcePath, data);
        
        return data;
    }
    
    /**
     * Verifica se textura ASTC existe (sem carregar)
     */
    public boolean hasAstcVersion(String resourcePath) {
        if (!initialized) {
            return false;
        }
        
        // Verificar cache primeiro
        if (textureCache.containsKey(resourcePath)) {
            return true;
        }
        
        // Verificar se arquivo existe
        String astcPath = "/assets/minecraft/" + resourcePath.replace(".png", ".astc");
        try {
            var stream = AstcTextureManager.class.getResourceAsStream(astcPath);
            if (stream != null) {
                stream.close();
                return true;
            }
        } catch (Exception e) {
            // Ignorar
        }
        
        return false;
    }
    
    /**
     * Limpa todo o cache (usado no reload de resource packs)
     */
    public synchronized void clearCache() {
        for (AstcTextureLoader.AstcTextureData data : textureCache.values()) {
            if (data != null) {
                data.close();
            }
        }
        textureCache.clear();
        Initializer.LOGGER.info("[ASTC] Cache limpo ({} texturas)", textureCache.size());
    }
}