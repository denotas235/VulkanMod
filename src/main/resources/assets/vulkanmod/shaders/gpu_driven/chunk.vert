#version 460
#extension GL_EXT_scalar_block_layout : require
#extension GL_EXT_shader_16bit_storage : require
#extension GL_EXT_shader_8bit_storage : require
#extension GL_EXT_shader_explicit_arithmetic_types : require
#extension GL_EXT_buffer_reference : require
#extension GL_KHR_shader_draw_parameters : require

struct CompactVertex {
    f16vec3 pos;        
    uint8_t ao;         
    uint8_t lightLevel; 
    f16vec2 uv;         
};

layout(scalar, binding = 0) readonly buffer VertexBuffer {
    CompactVertex verts[];
} vbo;

layout(scalar, binding = 1) readonly buffer ChunkPositions {
    vec3 positions[];
} chunkPosInfo;

layout(push_constant) uniform Matrices {
    mat4 viewProj;
} matrices;

layout(location = 0) out vec2 fragUV;
layout(location = 1) out float fragAO;
layout(location = 2) out float fragLight;

void main() {
    uint chunkId = gl_BaseInstanceARB;

    CompactVertex v = vbo.verts[gl_VertexIndex];

    vec3 chunkPos = chunkPosInfo.positions[chunkId];
    vec3 worldPos = vec3(v.pos) + chunkPos;

    gl_Position = matrices.viewProj * vec4(worldPos, 1.0);

    fragUV    = vec2(v.uv);
    fragAO    = float(v.ao) / 255.0;
    fragLight = float(v.lightLevel) / 255.0;
}
