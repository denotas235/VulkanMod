package net.vulkanmod.vulkan;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.vulkanmod.vulkan.memory.MemoryManager;
import net.vulkanmod.vulkan.queue.CommandPool;
import net.vulkanmod.vulkan.util.VUtil;
import net.vulkanmod.vulkan.timeline.TimelineSemaphoreManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK10.*;

/***
 * Synchronization utility to sync in frame ops that need to be completed before executing main cmd buffer.
 */
public class Synchronization {
    private static final int ALLOCATION_SIZE = 50;

    public static final Synchronization INSTANCE = new Synchronization(ALLOCATION_SIZE);

    private final LongBuffer fences;
    private int idx = 0;

    private final ObjectArrayList<CommandPool.CommandBuffer> fenceCbs = new ObjectArrayList<>();

    private final LongArrayList semaphores = new LongArrayList();
    private final ObjectArrayList<CommandPool.CommandBuffer> semaphoreCbs = new ObjectArrayList<>();

    // Timeline Semaphore Support
    private TimelineSemaphoreManager timelineSemaphore;
    private boolean timelineSupported = false;
    private boolean initialized = false;
    private long lastWaitValue = 0;
    private final LongArrayList waitTimelineValues = new LongArrayList();
    private ObjectArrayList<CommandPool.CommandBuffer>[] deferredResets;

    Synchronization(int allocSize) {
        this.fences = MemoryUtil.memAllocLong(allocSize);
    }

    public synchronized void init() {
        if (initialized) return;
        this.timelineSupported = Vulkan.getDevice().timelineSemaphoreSupported;
        int frames = Renderer.getFramesNum();
        this.deferredResets = new ObjectArrayList[frames];
        for (int i = 0; i < frames; i++) {
            this.deferredResets[i] = new ObjectArrayList<>();
        }
        if (this.timelineSupported) {
            this.timelineSemaphore = new TimelineSemaphoreManager();
        }
        this.initialized = true;
    }

    public boolean isTimelineSupported() {
        if (!initialized) init();
        return this.timelineSupported;
    }

    public long getTimelineSemaphore() {
        if (!initialized) init();
        return this.timelineSemaphore != null ? this.timelineSemaphore.getSemaphore() : VK_NULL_HANDLE;
    }

    public synchronized long incrementAndGetTimelineValue() {
        if (!initialized) init();
        return this.timelineSemaphore != null ? this.timelineSemaphore.getNextValue() : 0;
    }

    public synchronized void addWaitTimelineValue(long value) {
        this.waitTimelineValues.add(value);
    }

    public synchronized boolean hasTimelineWaits() {
        return !this.waitTimelineValues.isEmpty();
    }

    public synchronized long getMaxWaitTimelineValue() {
        long max = 0;
        for (int i = 0; i < this.waitTimelineValues.size(); i++) {
            long val = this.waitTimelineValues.getLong(i);
            if (val > max) max = val;
        }
        return max;
    }

    public synchronized void clearTimelineWaits() {
        this.waitTimelineValues.clear();
    }

    public synchronized void clearFrame(int frameIndex) {
        if (!initialized) init();
        if (timelineSupported) {
            deferredResets[frameIndex].forEach(CommandPool.CommandBuffer::reset);
            deferredResets[frameIndex].clear();
        }
    }

    public void addCommandBuffer(CommandPool.CommandBuffer commandBuffer) {
        addCommandBuffer(commandBuffer, false);
    }

    public synchronized void addCommandBuffer(CommandPool.CommandBuffer commandBuffer, boolean useSemaphore) {
        if (!initialized) init();

        if (timelineSupported) {
            deferredResets[Renderer.getCurrentFrame()].add(commandBuffer);
        } else {
            if (!useSemaphore) {
                this.addFence(commandBuffer.getFence());
                this.fenceCbs.add(commandBuffer);
            }
            else {
                this.semaphores.add(commandBuffer.getSemaphore());
                this.semaphoreCbs.add(commandBuffer);
            }
        }
    }

    public synchronized void addFence(long fence) {
        if (idx == ALLOCATION_SIZE)
            waitFences();

        fences.put(idx, fence);
        idx++;
    }

    public synchronized void waitFences() {
        if (!initialized) init();

        if (timelineSupported) {
            long maxWaitValue = this.timelineSemaphore.getLastSignaledValue();
            if (maxWaitValue > lastWaitValue) {
                this.timelineSemaphore.cpuWait(maxWaitValue, VUtil.UINT64_MAX);
                lastWaitValue = maxWaitValue;
            }
            idx = 0;
            return;
        }

        if (idx == 0)
            return;

        VkDevice device = Vulkan.getVkDevice();

        fences.limit(idx);

        vkWaitForFences(device, fences, true, VUtil.UINT64_MAX);

        this.fenceCbs.forEach(CommandPool.CommandBuffer::reset);
        this.fenceCbs.clear();

        fences.limit(ALLOCATION_SIZE);
        idx = 0;
    }

    public synchronized void addWaitSemaphore(long semaphore) {
        this.semaphores.add(semaphore);
    }

    public int getWaitSemaphoreCount() {
        return this.semaphores.size();
    }

    public void getWaitSemaphores(LongBuffer buffer) {
        buffer.put(this.semaphores.elements(), 0, this.semaphores.size());

        this.semaphores.clear();
    }

    public void scheduleCbReset() {
        if (timelineSupported) return;
        
        final var frameSemaphoreCbs = this.semaphoreCbs.clone();
        MemoryManager.getInstance().addFrameOp(
                () -> {
                    frameSemaphoreCbs.forEach(CommandPool.CommandBuffer::reset);
                }
        );

        this.semaphoreCbs.clear();
    }

    public static void waitFence(long fence) {
        VkDevice device = Vulkan.getVkDevice();

        vkWaitForFences(device, fence, true, VUtil.UINT64_MAX);
    }

    public static boolean checkFenceStatus(long fence) {
        VkDevice device = Vulkan.getVkDevice();
        return vkGetFenceStatus(device, fence) == VK_SUCCESS;
    }

}
