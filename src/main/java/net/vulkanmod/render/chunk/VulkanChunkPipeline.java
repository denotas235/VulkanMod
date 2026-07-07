package net.vulkanmod.render.chunk;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRDrawIndirectCount.*;
import net.vulkanmod.vulkan.Vulkan;

public class VulkanChunkPipeline {

    public static long getBufferDeviceAddress(long bufferHandle) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkBufferDeviceAddressInfoKHR info = VkBufferDeviceAddressInfoKHR.calloc(stack)
                .sType(org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO_KHR)
                .buffer(bufferHandle);
            return org.lwjgl.vulkan.KHRBufferDeviceAddress.vkGetBufferDeviceAddressKHR(Vulkan.getVkDevice(), info);
        }
    }

    public static void extractFrustumPlanes(org.joml.Matrix4f mvp, float[] planes) {
        // Left plane
        planes[0] = mvp.m03() + mvp.m00();
        planes[1] = mvp.m13() + mvp.m10();
        planes[2] = mvp.m23() + mvp.m20();
        planes[3] = mvp.m33() + mvp.m30();
        
        // Right plane
        planes[4] = mvp.m03() - mvp.m00();
        planes[5] = mvp.m13() - mvp.m10();
        planes[6] = mvp.m23() - mvp.m20();
        planes[7] = mvp.m33() - mvp.m30();
        
        // Bottom plane
        planes[8] = mvp.m03() + mvp.m01();
        planes[9] = mvp.m13() + mvp.m11();
        planes[10] = mvp.m23() + mvp.m21();
        planes[11] = mvp.m33() + mvp.m31();
        
        // Top plane
        planes[12] = mvp.m03() - mvp.m01();
        planes[13] = mvp.m13() - mvp.m11();
        planes[14] = mvp.m23() - mvp.m21();
        planes[15] = mvp.m33() - mvp.m31();
        
        // Near plane
        planes[16] = mvp.m03() + mvp.m02();
        planes[17] = mvp.m13() + mvp.m12();
        planes[18] = mvp.m23() + mvp.m22();
        planes[19] = mvp.m33() + mvp.m32();
        
        // Far plane
        planes[20] = mvp.m03() - mvp.m02();
        planes[21] = mvp.m13() - mvp.m12();
        planes[22] = mvp.m23() - mvp.m22();
        planes[23] = mvp.m33() - mvp.m32();
        
        // Normalize the planes
        for (int i = 0; i < 6; i++) {
            float len = (float) Math.sqrt(planes[i*4] * planes[i*4] + planes[i*4+1] * planes[i*4+1] + planes[i*4+2] * planes[i*4+2]);
            planes[i*4] /= len;
            planes[i*4+1] /= len;
            planes[i*4+2] /= len;
            planes[i*4+3] /= len;
        }
    }

    /**
     * Records culling compute shader execution followed by the indirect draw execution on the GPU.
     */
    public void recordRenderCommands(
            VkCommandBuffer commandBuffer,
            long pipelineLayout,
            long computePipeline,
            long graphicsPipeline,
            long indirectBufferHandle,
            long indirectBufferOffset,
            long drawCountBufferHandle,
            long drawCountBufferOffset,
            long aabbPtr,
            long meshInfoPtr,
            long indirectPtr,
            long countPtr,
            int totalChunks,
            float[] frustumPlanes) {

        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_COMPUTE, computePipeline);

        // Clear the draw counter in VRAM
        vkCmdFillBuffer(commandBuffer, drawCountBufferHandle, drawCountBufferOffset, 4L, 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Memory barrier to guarantee clear completes before shader reads it
            VkMemoryBarrier.Buffer memBarrier = VkMemoryBarrier.calloc(1, stack)
                .sType$Default()
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_SHADER_READ_BIT | VK_ACCESS_SHADER_WRITE_BIT);

            vkCmdPipelineBarrier(
                commandBuffer,
                VK_PIPELINE_STAGE_TRANSFER_BIT,
                VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                0, memBarrier, null, null);

            // Write Push Constants (144 bytes)
            ByteBuffer pushConstants = stack.malloc(144);
            pushConstants.putLong(0, aabbPtr);
            pushConstants.putLong(8, meshInfoPtr);
            pushConstants.putLong(16, indirectPtr);
            pushConstants.putLong(24, countPtr);
            pushConstants.putInt(32, totalChunks);
            // Padding at 36-47
            for (int i = 0; i < 24; i++) {
                pushConstants.putFloat(48 + i * 4, frustumPlanes[i]);
            }
            
            vkCmdPushConstants(commandBuffer, pipelineLayout, VK_SHADER_STAGE_COMPUTE_BIT, 0, pushConstants);

            // Compute Cull
            int workgroups = (int) Math.ceil(totalChunks / 64.0);
            vkCmdDispatch(commandBuffer, workgroups, 1, 1);

            // Pipeline barriers (Compute -> Graphics Draw)
            VkBufferMemoryBarrier.Buffer indirectBarrier = VkBufferMemoryBarrier.calloc(2, stack);

            // Indirect commands buffer barrier
            indirectBarrier.get(0)
                .sType$Default()
                .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(indirectBufferHandle)
                .offset(indirectBufferOffset)
                .size(totalChunks * 20L); 

            // Draw Count buffer barrier
            indirectBarrier.get(1)
                .sType$Default()
                .srcAccessMask(VK_ACCESS_SHADER_WRITE_BIT)
                .dstAccessMask(VK_ACCESS_INDIRECT_COMMAND_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(drawCountBufferHandle)
                .offset(drawCountBufferOffset)
                .size(4);

            vkCmdPipelineBarrier(
                commandBuffer,
                VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT,
                VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT,
                0, null, indirectBarrier, null);

            // Draw call
            vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

            vkCmdDrawIndexedIndirectCountKHR(
                commandBuffer,
                indirectBufferHandle,
                indirectBufferOffset,
                drawCountBufferHandle,
                drawCountBufferOffset,
                totalChunks,
                20
            );
        }
    }
}
