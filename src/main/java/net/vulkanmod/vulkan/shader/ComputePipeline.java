package net.vulkanmod.vulkan.shader;

import org.lwjgl.vulkan.*;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.Vulkan;
import net.vulkanmod.vulkan.device.DeviceManager;
import net.vulkanmod.vulkan.memory.buffer.UniformBuffer;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class ComputePipeline extends Pipeline {
    private final long pipeline;
    private long shaderModule = 0L;

    public ComputePipeline(Pipeline.Builder builder) {
        super(builder.name);
        this.buffers = builder.getUBOs();
        this.imageDescriptors = builder.getImageDescriptors();
        this.pushConstants = builder.getPushConstants();
        this.createDescriptorSetLayout();
        this.createPipelineLayout();
        
        String csh = builder.getShadersSrc().get(SPIRVUtils.ShaderKind.COMPUTE_SHADER);
        SPIRVUtils.SPIRV spirv = SPIRVUtils.compileShader(String.format("%s.comp", this.name), csh, SPIRVUtils.ShaderKind.COMPUTE_SHADER);
        this.shaderModule = Pipeline.createShaderModule(spirv.bytecode());
        this.pipeline = this.createPipeline();
        this.createDescriptorSets(Renderer.getFramesNum());
        PIPELINES.add(this);
    }

    private long createPipeline() {
        try (MemoryStack stack = stackPush()) {
            ByteBuffer entryPoint = stack.UTF8("main");
            VkPipelineShaderStageCreateInfo shaderStage = VkPipelineShaderStageCreateInfo.calloc(stack);
            shaderStage.sType$Default();
            shaderStage.stage(VK_SHADER_STAGE_COMPUTE_BIT);
            shaderStage.module(this.shaderModule);
            shaderStage.pName(entryPoint);
            
            VkComputePipelineCreateInfo.Buffer pipelineInfo = VkComputePipelineCreateInfo.calloc(1, stack);
            pipelineInfo.sType$Default();
            pipelineInfo.stage(shaderStage);
            pipelineInfo.layout(this.pipelineLayout);
            pipelineInfo.basePipelineHandle(VK_NULL_HANDLE);
            pipelineInfo.basePipelineIndex(-1);
            
            LongBuffer pipeline = stack.mallocLong(1);
            int res = vkCreateComputePipelines(Vulkan.getVkDevice(), PIPELINE_CACHE, pipelineInfo, null, pipeline);
            Vulkan.checkResult(res, "Failed to create compute pipeline");
            
            return pipeline.get(0);
        }
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, int frame) {
        UniformBuffer uniformBuffer = Renderer.getDrawer().getUniformBuffer();
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_COMPUTE);
    }

    public void bindDescriptorSets(VkCommandBuffer commandBuffer, UniformBuffer uniformBuffer, int frame) {
        this.descriptorSets[frame].bindSets(commandBuffer, uniformBuffer, VK_PIPELINE_BIND_POINT_COMPUTE);
    }

    public void cleanUp() {
        vkDestroyShaderModule(Vulkan.getVkDevice(), this.shaderModule, null);
        this.destroyDescriptorSets();
        vkDestroyPipeline(Vulkan.getVkDevice(), this.pipeline, null);
        vkDestroyDescriptorSetLayout(Vulkan.getVkDevice(), this.descriptorSetLayout, null);
        vkDestroyPipelineLayout(Vulkan.getVkDevice(), this.pipelineLayout, null);
        PIPELINES.remove(this);
        Renderer.getInstance().removeUsedPipeline(this);
    }

    public long getId() {
        return this.pipeline;
    }
}
