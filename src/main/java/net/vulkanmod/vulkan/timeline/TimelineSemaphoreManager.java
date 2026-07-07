package net.vulkanmod.vulkan.timeline;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.LongBuffer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.*;

public class TimelineSemaphoreManager {
    private final long semaphore;
    private final VkDevice device;
    private long lastSignaledValue = 0;

    public TimelineSemaphoreManager() {
        this.device = DeviceManager.vkDevice;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            // Configure Timeline Type Info
            VkSemaphoreTypeCreateInfo timelineTypeInfo = VkSemaphoreTypeCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR)
                .semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE_KHR)
                .initialValue(0);

            // Configure Semaphore Info
            VkSemaphoreCreateInfo createInfo = VkSemaphoreCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO)
                .pNext(timelineTypeInfo.address());

            LongBuffer pSemaphore = stack.mallocLong(1);
            int res = vkCreateSemaphore(device, createInfo, null, pSemaphore);
            Vulkan.checkResult(res, "Failed to create timeline semaphore");
            
            this.semaphore = pSemaphore.get(0);
        }
    }

    public long getSemaphore() {
        return this.semaphore;
    }

    /**
     * Get the current value of the semaphore on the GPU.
     */
    public long getCounterValue() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pValue = stack.mallocLong(1);
            int res = vkGetSemaphoreCounterValueKHR(device, semaphore, pValue);
            Vulkan.checkResult(res, "Failed to get semaphore counter value");
            return pValue.get(0);
        }
    }

    /**
     * Blocks the CPU until the semaphore reaches or exceeds the specified value.
     */
    public void cpuWait(long value, long timeoutNanoseconds) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreWaitInfo waitInfo = VkSemaphoreWaitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO_KHR)
                .pSemaphores(stack.longs(semaphore))
                .pValues(stack.longs(value));

            int res = vkWaitSemaphoresKHR(device, waitInfo, timeoutNanoseconds);
            Vulkan.checkResult(res, "Failed waiting for timeline semaphore on CPU");
        }
    }

    /**
     * Manually signals the semaphore from the CPU (useful for debugging/resets).
     */
    public void cpuSignal(long value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkSemaphoreSignalInfo signalInfo = VkSemaphoreSignalInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO_KHR)
                .semaphore(semaphore)
                .value(value);

            int res = vkSignalSemaphoreKHR(device, signalInfo);
            Vulkan.checkResult(res, "Failed signaling timeline semaphore from CPU");
            this.lastSignaledValue = value;
        }
    }

    /**
     * Chins VkTimelineSemaphoreSubmitInfo onto a VkSubmitInfo.Builder on the stack.
     */
    public static void configureSubmitSync(
            MemoryStack stack,
            VkSubmitInfo submitInfo,
            long waitSemaphore, long waitValue, int waitStageFlags,
            long signalSemaphore, long signalValue) {

        VkTimelineSemaphoreSubmitInfo submitTimelineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack)
            .sType(VK_STRUCTURE_TYPE_TIMELINE_SEMAPHORE_SUBMIT_INFO_KHR);

        if (waitSemaphore != VK_NULL_HANDLE) {
            submitInfo.pWaitSemaphores(stack.longs(waitSemaphore));
            submitInfo.pWaitDstStageMask(stack.ints(waitStageFlags));
            submitTimelineInfo.pWaitSemaphoreValues(stack.longs(waitValue));
        }

        if (signalSemaphore != VK_NULL_HANDLE) {
            submitInfo.pSignalSemaphores(stack.longs(signalSemaphore));
            submitTimelineInfo.pSignalSemaphoreValues(stack.longs(signalValue));
        }

        submitInfo.pNext(submitTimelineInfo.address());
    }

    public long getNextValue() {
        return ++lastSignaledValue;
    }

    public long getLastSignaledValue() {
        return lastSignaledValue;
    }

    public void destroy() {
        vkDestroySemaphore(device, semaphore, null);
    }
}
