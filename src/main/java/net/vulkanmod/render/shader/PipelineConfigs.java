package net.vulkanmod.render.shader;

import net.vulkanmod.vulkan.shader.PipelineConfig;
import net.vulkanmod.vulkan.shader.SPIRVUtils;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_ALL_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;

public class PipelineConfigs {
    public static final PipelineConfig.UB TERRAIN_UB0 = PipelineConfig.UB.builder(0, VK_SHADER_STAGE_VERTEX_BIT)
                                                    .addUniform("mat4", "MVP")
                                                    .addUniform("int", "CurrentTime")
                                                    .build();

    public static final PipelineConfig.UB TERRAIN_UB1 = PipelineConfig.UB.builder(1, VK_SHADER_STAGE_ALL_GRAPHICS)
                                                                  .addUniform("vec4", "FogColor")
                                                                  .addUniform("float", "FogEnvironmentalStart")
                                                                  .addUniform("float", "FogEnvironmentalEnd")
                                                                  .addUniform("float", "FogRenderDistanceStart")
                                                                  .addUniform("float", "FogRenderDistanceEnd")
                                                                  .addUniform("float", "FogSkyEnd")
                                                                  .addUniform("float", "FogCloudsEnd")
                                                                  .addUniform("float", "AlphaCutout")
                                                                  .addUniform("vec2", "TextureSize")
                                                                  .addUniform("vec2", "TexelSize")
                                                                  .addUniform("int", "UseRgss")
                                                                  .build();

    public static final PipelineConfig.UB TERRAIN_UB2 = PipelineConfig.UB.builder(2, VK_SHADER_STAGE_VERTEX_BIT)
                                                    .setSize(4096)
                                                    .build();

    public static final PipelineConfig.UB TERRAIN_PC = PipelineConfig.UB.builder(0, VK_SHADER_STAGE_VERTEX_BIT) // Binding ignored
                                                                 .addUniform("vec3", "ModelOffset")
                                                                  .build();

    public static final PipelineConfig TERRAIN = PipelineConfig.builder()
                                                        .withShader(SPIRVUtils.ShaderKind.VERTEX_SHADER, "terrain/terrain")
                                                        .withShader(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, "terrain/terrain")
                                                        .addUB(TERRAIN_UB0)
                                                        .addUB(TERRAIN_UB1)
                                                        .addUB(TERRAIN_UB2)
                                                        .setPushConstants(TERRAIN_PC)
                                                        .addImageDescriptor(3, "sampler2D", "Sampler0", 0)
                                                        .addImageDescriptor(4, "sampler2D", "LightTexture", 2)
                                                        .build();

    static final PipelineConfig TERRAIN_EARLY_Z_CONFIG = PipelineConfig.builder()
                                                                       .withShader(SPIRVUtils.ShaderKind.VERTEX_SHADER, "terrain/terrain")
                                                                       .withShader(SPIRVUtils.ShaderKind.FRAGMENT_SHADER, "terrain_earlyZ/terrain_earlyZ")
                                                                       .addUB(TERRAIN_UB0)
                                                                       .addUB(TERRAIN_UB1)
                                                                       .addUB(TERRAIN_UB2)
                                                                       .setPushConstants(TERRAIN_PC)
                                                                       .addImageDescriptor(3, "sampler2D", "Sampler0", 0)
                                                                       .addImageDescriptor(4, "sampler2D", "LightTexture", 2)
                                                                       .build();

    public static final PipelineConfig.UB CULLING_PC = PipelineConfig.UB.builder(0, org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_COMPUTE_BIT)
                                                    .addUniform("vec2", "aabbPtr")
                                                    .addUniform("vec2", "meshInfoPtr")
                                                    .addUniform("vec2", "indirectPtr")
                                                    .addUniform("vec2", "countPtr")
                                                    .addUniform("int", "totalChunks")
                                                    .addUniform("vec3", "padding")
                                                    .addUniform("vec4", "plane0")
                                                    .addUniform("vec4", "plane1")
                                                    .addUniform("vec4", "plane2")
                                                    .addUniform("vec4", "plane3")
                                                    .addUniform("vec4", "plane4")
                                                    .addUniform("vec4", "plane5")
                                                    .build();

    public static final PipelineConfig CULLING = PipelineConfig.builder()
                                                        .withShader(SPIRVUtils.ShaderKind.COMPUTE_SHADER, "gpu_driven/culling")
                                                        .setPushConstants(CULLING_PC)
                                                        .build();

}
