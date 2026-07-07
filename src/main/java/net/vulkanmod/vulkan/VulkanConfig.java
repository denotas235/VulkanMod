package net.vulkanmod.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.*;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.*;
import static org.lwjgl.vulkan.EXTCustomBorderColor.*;
import static org.lwjgl.vulkan.EXTLineRasterization.*;
import static org.lwjgl.vulkan.EXTProvokingVertex.*;

public class VulkanConfig {

    public static final String[] DESIRED_INSTANCE_EXTENSIONS = {
        "VK_KHR_surface",
        "VK_KHR_android_surface",
        "VK_EXT_debug_report",
        "VK_EXT_swapchain_colorspace",
        "VK_KHR_device_group_creation",
        "VK_KHR_external_fence_capabilities",
        "VK_KHR_external_memory_capabilities",
        "VK_KHR_external_semaphore_capabilities",
        "VK_KHR_get_physical_device_properties2",
        "VK_KHR_get_surface_capabilities2"
    };

    public static final String[] DESIRED_DEVICE_EXTENSIONS = {
        "VK_KHR_swapchain",
        "VK_KHR_timeline_semaphore",
        "VK_KHR_dedicated_allocation",
        "VK_KHR_bind_memory2",
        "VK_KHR_get_memory_requirements2",
        "VK_KHR_maintenance1",
        "VK_KHR_maintenance2",
        "VK_KHR_maintenance3",
        "VK_KHR_external_memory",
        "VK_KHR_external_semaphore",
        "VK_KHR_external_fence",
        "VK_KHR_descriptor_update_template",
        "VK_KHR_create_renderpass2",
        "VK_KHR_depth_stencil_resolve",
        "VK_KHR_imageless_framebuffer",
        "VK_KHR_image_format_list",
        "VK_KHR_shader_draw_parameters",
        "VK_KHR_uniform_buffer_standard_layout",
        "VK_KHR_buffer_device_address",
        "VK_KHR_separate_depth_stencil_layouts",
        "VK_EXT_host_query_reset",
        "VK_EXT_index_type_uint8",
        "VK_EXT_scalar_block_layout",
        "VK_EXT_image_robustness",
        "VK_KHR_shader_float16_int8",
        "VK_KHR_8bit_storage",
        "VK_KHR_16bit_storage",
        "VK_KHR_multiview",
        "VK_KHR_sampler_ycbcr_conversion",
        "VK_KHR_variable_pointers",
        "VK_KHR_relaxed_block_layout",
        "VK_KHR_shader_subgroup_extended_types",
        "VK_KHR_vulkan_memory_model",
        "VK_KHR_spirv_1_4",
        "VK_KHR_shared_presentable_image",
        "VK_KHR_incremental_present",
        "VK_KHR_sampler_mirror_clamp_to_edge",
        "VK_EXT_custom_border_color",
        "VK_EXT_device_memory_report",
        "VK_EXT_global_priority",
        "VK_EXT_external_memory_dma_buf",
        "VK_EXT_queue_family_foreign",
        "VK_EXT_4444_formats",
        "VK_EXT_astc_decode_mode",
        "VK_EXT_separate_stencil_usage",
        "VK_EXT_shader_subgroup_ballot",
        "VK_EXT_shader_subgroup_vote",
        "VK_EXT_subgroup_size_control",
        "VK_EXT_texture_compression_astc_hdr",
        "VK_EXT_transform_feedback",
        "VK_EXT_line_rasterization",
        "VK_EXT_provoking_vertex",
        "VK_EXT_inline_uniform_block",
        "VK_EXT_image_drm_format_modifier",
        "VK_EXT_calibrated_timestamps",
        "VK_ANDROID_external_memory_android_hardware_buffer",
        "VK_GOOGLE_display_timing",
        "VK_KHR_device_group",
        "VK_KHR_driver_properties",
        "VK_KHR_external_fence_fd",
        "VK_KHR_external_memory_fd",
        "VK_KHR_external_semaphore_fd",
        "VK_KHR_shader_float_controls",
        "VK_KHR_shader_non_semantic_info"
    };

    /**
     * Query available instance extensions and return those that are desired and supported.
     */
    public static Set<String> getSupportedInstanceExtensions(PointerBuffer glfwExtensions) {
        Set<String> supported = new HashSet<>();
        
        // Add required GLFW extensions
        if (glfwExtensions != null) {
            for (int i = 0; i < glfwExtensions.capacity(); i++) {
                supported.add(glfwExtensions.getStringUTF8(i));
            }
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            vkEnumerateInstanceExtensionProperties((ByteBuffer) null, count, null);
            
            if (count.get(0) > 0) {
                VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0), stack);
                vkEnumerateInstanceExtensionProperties((ByteBuffer) null, count, properties);
                
                Set<String> available = new HashSet<>();
                for (int i = 0; i < properties.capacity(); i++) {
                    available.add(properties.get(i).extensionNameString());
                }

                for (String ext : DESIRED_INSTANCE_EXTENSIONS) {
                    if (available.contains(ext)) {
                        supported.add(ext);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return supported;
    }

    /**
     * Query available device extensions and return those that are desired and supported.
     */
    public static Set<String> getSupportedDeviceExtensions(VkPhysicalDevice physicalDevice) {
        Set<String> supported = new HashSet<>();
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer count = stack.ints(0);
            vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, null);
            
            if (count.get(0) > 0) {
                VkExtensionProperties.Buffer properties = VkExtensionProperties.malloc(count.get(0), stack);
                vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer) null, count, properties);
                
                Set<String> available = new HashSet<>();
                for (int i = 0; i < properties.capacity(); i++) {
                    available.add(properties.get(i).extensionNameString());
                }

                for (String ext : DESIRED_DEVICE_EXTENSIONS) {
                    if (available.contains(ext)) {
                        supported.add(ext);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return supported;
    }

    /**
     * Builds the pNext features chain dynamically using the stack provided.
     * Only enables features that are supported by the physical device.
     */
    public static long buildFeatureChain(VkPhysicalDevice physicalDevice, MemoryStack stack, Set<String> enabledExtensions) {
        long pNext = 0;

        // Query what the physical device features actually support
        VkPhysicalDeviceFeatures2 deviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack);
        deviceFeatures2.sType$Default();

        // Chains for querying capabilities
        VkPhysicalDeviceTimelineSemaphoreFeatures timelineQuery = VkPhysicalDeviceTimelineSemaphoreFeatures.calloc(stack);
        timelineQuery.sType$Default();
        deviceFeatures2.pNext(timelineQuery);

        VkPhysicalDeviceBufferDeviceAddressFeatures bdaQuery = VkPhysicalDeviceBufferDeviceAddressFeatures.calloc(stack);
        bdaQuery.sType$Default();
        timelineQuery.pNext(bdaQuery.address());

        VkPhysicalDeviceShaderFloat16Int8Features float16Query = VkPhysicalDeviceShaderFloat16Int8Features.calloc(stack);
        float16Query.sType$Default();
        bdaQuery.pNext(float16Query.address());

        VkPhysicalDevice8BitStorageFeatures storage8Query = VkPhysicalDevice8BitStorageFeatures.calloc(stack);
        storage8Query.sType$Default();
        float16Query.pNext(storage8Query.address());

        VkPhysicalDevice16BitStorageFeatures storage16Query = VkPhysicalDevice16BitStorageFeatures.calloc(stack);
        storage16Query.sType$Default();
        storage8Query.pNext(storage16Query.address());

        VkPhysicalDeviceMultiviewFeatures multiviewQuery = VkPhysicalDeviceMultiviewFeatures.calloc(stack);
        multiviewQuery.sType$Default();
        storage16Query.pNext(multiviewQuery.address());

        VkPhysicalDeviceVariablePointersFeatures varPointersQuery = VkPhysicalDeviceVariablePointersFeatures.calloc(stack);
        varPointersQuery.sType$Default();
        multiviewQuery.pNext(varPointersQuery.address());

        VkPhysicalDeviceScalarBlockLayoutFeatures scalarQuery = VkPhysicalDeviceScalarBlockLayoutFeatures.calloc(stack);
        scalarQuery.sType$Default();
        varPointersQuery.pNext(scalarQuery.address());

        VkPhysicalDeviceUniformBufferStandardLayoutFeatures uboQuery = VkPhysicalDeviceUniformBufferStandardLayoutFeatures.calloc(stack);
        uboQuery.sType$Default();
        scalarQuery.pNext(uboQuery.address());

        VkPhysicalDeviceImagelessFramebufferFeatures imagelessQuery = VkPhysicalDeviceImagelessFramebufferFeatures.calloc(stack);
        imagelessQuery.sType$Default();
        uboQuery.pNext(imagelessQuery.address());

        VkPhysicalDeviceSeparateDepthStencilLayoutsFeatures sepDepthQuery = VkPhysicalDeviceSeparateDepthStencilLayoutsFeatures.calloc(stack);
        sepDepthQuery.sType$Default();
        imagelessQuery.pNext(sepDepthQuery.address());

        VkPhysicalDeviceHostQueryResetFeatures hostQuery = VkPhysicalDeviceHostQueryResetFeatures.calloc(stack);
        hostQuery.sType$Default();
        sepDepthQuery.pNext(hostQuery.address());

        VkPhysicalDeviceVulkanMemoryModelFeatures memoryModelQuery = VkPhysicalDeviceVulkanMemoryModelFeatures.calloc(stack);
        memoryModelQuery.sType$Default();
        hostQuery.pNext(memoryModelQuery.address());

        VkPhysicalDeviceSamplerYcbcrConversionFeatures ycbcrQuery = VkPhysicalDeviceSamplerYcbcrConversionFeatures.calloc(stack);
        ycbcrQuery.sType$Default();
        memoryModelQuery.pNext(ycbcrQuery.address());

        // Perform the query on the hardware
        vkGetPhysicalDeviceFeatures2(physicalDevice, deviceFeatures2);

        // Build the active enabled chain based on query results and enabled extensions
        if (enabledExtensions.contains("VK_KHR_timeline_semaphore") && timelineQuery.timelineSemaphore()) {
            VkPhysicalDeviceTimelineSemaphoreFeatures timeline = VkPhysicalDeviceTimelineSemaphoreFeatures.calloc(stack)
                .sType$Default()
                .timelineSemaphore(true);
            timeline.pNext(pNext);
            pNext = timeline.address();
        }

        if (enabledExtensions.contains("VK_KHR_buffer_device_address") && bdaQuery.bufferDeviceAddress()) {
            VkPhysicalDeviceBufferDeviceAddressFeatures bda = VkPhysicalDeviceBufferDeviceAddressFeatures.calloc(stack)
                .sType$Default()
                .bufferDeviceAddress(true);
            bda.pNext(pNext);
            pNext = bda.address();
        }

        if (enabledExtensions.contains("VK_KHR_shader_float16_int8") && (float16Query.shaderFloat16() || float16Query.shaderInt8())) {
            VkPhysicalDeviceShaderFloat16Int8Features float16 = VkPhysicalDeviceShaderFloat16Int8Features.calloc(stack)
                .sType$Default()
                .shaderFloat16(float16Query.shaderFloat16())
                .shaderInt8(float16Query.shaderInt8());
            float16.pNext(pNext);
            pNext = float16.address();
        }

        if (enabledExtensions.contains("VK_KHR_8bit_storage") && storage8Query.storageBuffer8BitAccess()) {
            VkPhysicalDevice8BitStorageFeatures storage8 = VkPhysicalDevice8BitStorageFeatures.calloc(stack)
                .sType$Default()
                .storageBuffer8BitAccess(true)
                .uniformAndStorageBuffer8BitAccess(storage8Query.uniformAndStorageBuffer8BitAccess())
                .storagePushConstant8(storage8Query.storagePushConstant8());
            storage8.pNext(pNext);
            pNext = storage8.address();
        }

        if (enabledExtensions.contains("VK_KHR_16bit_storage") && storage16Query.storageBuffer16BitAccess()) {
            VkPhysicalDevice16BitStorageFeatures storage16 = VkPhysicalDevice16BitStorageFeatures.calloc(stack)
                .sType$Default()
                .storageBuffer16BitAccess(true)
                .uniformAndStorageBuffer16BitAccess(storage16Query.uniformAndStorageBuffer16BitAccess())
                .storagePushConstant16(storage16Query.storagePushConstant16())
                .storageInputOutput16(storage16Query.storageInputOutput16());
            storage16.pNext(pNext);
            pNext = storage16.address();
        }

        if (enabledExtensions.contains("VK_KHR_multiview") && multiviewQuery.multiview()) {
            VkPhysicalDeviceMultiviewFeatures multiview = VkPhysicalDeviceMultiviewFeatures.calloc(stack)
                .sType$Default()
                .multiview(true);
            multiview.pNext(pNext);
            pNext = multiview.address();
        }

        if (enabledExtensions.contains("VK_KHR_variable_pointers") && varPointersQuery.variablePointers()) {
            VkPhysicalDeviceVariablePointersFeatures varPointers = VkPhysicalDeviceVariablePointersFeatures.calloc(stack)
                .sType$Default()
                .variablePointers(true)
                .variablePointersStorageBuffer(varPointersQuery.variablePointersStorageBuffer());
            varPointers.pNext(pNext);
            pNext = varPointers.address();
        }

        if (enabledExtensions.contains("VK_EXT_scalar_block_layout") && scalarQuery.scalarBlockLayout()) {
            VkPhysicalDeviceScalarBlockLayoutFeatures scalar = VkPhysicalDeviceScalarBlockLayoutFeatures.calloc(stack)
                .sType$Default()
                .scalarBlockLayout(true);
            scalar.pNext(pNext);
            pNext = scalar.address();
        }

        if (enabledExtensions.contains("VK_KHR_uniform_buffer_standard_layout") && uboQuery.uniformBufferStandardLayout()) {
            VkPhysicalDeviceUniformBufferStandardLayoutFeatures ubo = VkPhysicalDeviceUniformBufferStandardLayoutFeatures.calloc(stack)
                .sType$Default()
                .uniformBufferStandardLayout(true);
            ubo.pNext(pNext);
            pNext = ubo.address();
        }

        if (enabledExtensions.contains("VK_KHR_imageless_framebuffer") && imagelessQuery.imagelessFramebuffer()) {
            VkPhysicalDeviceImagelessFramebufferFeatures imageless = VkPhysicalDeviceImagelessFramebufferFeatures.calloc(stack)
                .sType$Default()
                .imagelessFramebuffer(true);
            imageless.pNext(pNext);
            pNext = imageless.address();
        }

        if (enabledExtensions.contains("VK_KHR_separate_depth_stencil_layouts") && sepDepthQuery.separateDepthStencilLayouts()) {
            VkPhysicalDeviceSeparateDepthStencilLayoutsFeatures sepDepth = VkPhysicalDeviceSeparateDepthStencilLayoutsFeatures.calloc(stack)
                .sType$Default()
                .separateDepthStencilLayouts(true);
            sepDepth.pNext(pNext);
            pNext = sepDepth.address();
        }

        if (enabledExtensions.contains("VK_EXT_host_query_reset") && hostQuery.hostQueryReset()) {
            VkPhysicalDeviceHostQueryResetFeatures host = VkPhysicalDeviceHostQueryResetFeatures.calloc(stack)
                .sType$Default()
                .hostQueryReset(true);
            host.pNext(pNext);
            pNext = host.address();
        }

        if (enabledExtensions.contains("VK_KHR_vulkan_memory_model") && memoryModelQuery.vulkanMemoryModel()) {
            VkPhysicalDeviceVulkanMemoryModelFeatures memoryModel = VkPhysicalDeviceVulkanMemoryModelFeatures.calloc(stack)
                .sType$Default()
                .vulkanMemoryModel(true)
                .vulkanMemoryModelAvailabilityVisibilityChains(memoryModelQuery.vulkanMemoryModelAvailabilityVisibilityChains())
                .vulkanMemoryModelDeviceScope(memoryModelQuery.vulkanMemoryModelDeviceScope());
            memoryModel.pNext(pNext);
            pNext = memoryModel.address();
        }

        if (enabledExtensions.contains("VK_KHR_sampler_ycbcr_conversion") && ycbcrQuery.samplerYcbcrConversion()) {
            VkPhysicalDeviceSamplerYcbcrConversionFeatures ycbcr = VkPhysicalDeviceSamplerYcbcrConversionFeatures.calloc(stack)
                .sType$Default()
                .samplerYcbcrConversion(true);
            ycbcr.pNext(pNext);
            pNext = ycbcr.address();
        }

        return pNext;
    }
}
