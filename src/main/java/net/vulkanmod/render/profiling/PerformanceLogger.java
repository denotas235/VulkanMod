package net.vulkanmod.render.profiling;

import net.minecraft.client.Minecraft;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.render.chunk.build.task.TaskDispatcher;
import net.vulkanmod.vulkan.Renderer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PerformanceLogger {
    private static PerformanceLogger INSTANCE;

    private final File logFile;
    private PrintWriter writer;
    private long startTime;
    private long lastLogTime = 0;

    private int framesThisSecond = 0;
    private long lastFpsUpdateTime = 0;
    private float currentFps = 0.0f;

    public static PerformanceLogger getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PerformanceLogger();
        }
        return INSTANCE;
    }

    private PerformanceLogger() {
        File gameDir = Minecraft.getInstance().gameDirectory;
        File logsDir = new File(gameDir, "logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        this.logFile = new File(logsDir, "vulkanmod_perf_" + timeStamp + ".csv");
        this.startTime = System.currentTimeMillis();
        this.lastFpsUpdateTime = this.startTime;
        this.lastLogTime = this.startTime;

        try {
            this.writer = new PrintWriter(new FileWriter(logFile, true));
            // CSV Header
            this.writer.println("Timestamp_ms,FPS,FrameTime_ms,RenderedSections,CompiledSections,TotalSections,QueuedTasks,AllocatedMemory_MB,FreeMemory_MB,MaxMemory_MB");
            this.writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void countFrame(long frameTimeNs) {
        this.framesThisSecond++;
        long now = System.currentTimeMillis();
        
        long elapsedFps = now - this.lastFpsUpdateTime;
        if (elapsedFps >= 1000) {
            this.currentFps = (this.framesThisSecond * 1000.0f) / elapsedFps;
            this.framesThisSecond = 0;
            this.lastFpsUpdateTime = now;
        }

        if (now - this.lastLogTime >= 1000) {
            this.logMetrics(now, frameTimeNs);
            this.lastLogTime = now;
        }
    }

    private void logMetrics(long now, long lastFrameTimeNs) {
        if (this.writer == null) return;

        long elapsed = now - this.startTime;
        float frameTimeMs = lastFrameTimeNs / 1_000_000.0f;

        int renderedSections = 0;
        int compiledSections = 0;
        int totalSections = 0;
        int queuedTasks = 0;

        WorldRenderer worldRenderer = WorldRenderer.getInstance();
        if (worldRenderer != null) {
            renderedSections = worldRenderer.getVisibleSectionsCount();
            compiledSections = worldRenderer.getNonEmptyChunks();
            
            if (worldRenderer.getSectionGrid() != null) {
                totalSections = worldRenderer.getSectionGrid().getSectionCount();
            }
            
            TaskDispatcher dispatcher = worldRenderer.getTaskDispatcher();
            if (dispatcher != null) {
                // Check queued task size if possible, otherwise we check if running
                queuedTasks = dispatcher.isIdle() ? 0 : 1; // Basic busy flag
            }
        }

        Runtime runtime = Runtime.getRuntime();
        long maxMem = runtime.maxMemory() / (1024 * 1024);
        long totalMem = runtime.totalMemory() / (1024 * 1024);
        long freeMem = runtime.freeMemory() / (1024 * 1024);
        long allocMem = totalMem - freeMem;

        this.writer.printf("%d,%.1f,%.2f,%d,%d,%d,%d,%d,%d,%d\n",
                elapsed,
                this.currentFps,
                frameTimeMs,
                renderedSections,
                compiledSections,
                totalSections,
                queuedTasks,
                allocMem,
                freeMem,
                maxMem
        );
        this.writer.flush();
    }

    public void close() {
        if (this.writer != null) {
            this.writer.close();
            this.writer = null;
        }
    }
}
