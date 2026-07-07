package net.vulkanmod.render.chunk;

import java.nio.ByteBuffer;

public class ChunkVertexEncoder {

    public static final int VERTEX_SIZE_BYTES = 12;

    /**
     * Packs MC data into the Vulkan 12-byte struct layout.
     */
    public static void putCompactVertex(
            ByteBuffer buffer,
            float x, float y, float z,
            int ao, int light,
            float u, float v) {

        // Position: f16vec3 (6 bytes)
        buffer.putShort(Float.floatToFloat16(x));
        buffer.putShort(Float.floatToFloat16(y));
        buffer.putShort(Float.floatToFloat16(z));

        // AO and Light: uint8_t (2 bytes)
        buffer.put((byte) (ao & 0xFF));
        buffer.put((byte) (light & 0xFF));

        // UV texture: f16vec2 (4 bytes)
        buffer.putShort(Float.floatToFloat16(u));
        buffer.putShort(Float.floatToFloat16(v));
    }
}
