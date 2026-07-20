package com.cms.service;

import com.cms.util.FileHandler;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Writes user-activity events to activity.log on a dedicated background
 * thread so logging never blocks the main console interaction loop.
 */
public class AsyncLoggerService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "activity-logger");
        t.setDaemon(true);
        return t;
    });

    public AsyncLoggerService(Path logFile) {
        this.logFile = logFile;
    }

    public void log(String actor, String action) {
        String timestamp = LocalDateTime.now().format(TS_FMT);
        String line = String.format("[%s] %s :: %s", timestamp, actor, action);
        executor.submit(() -> {
            try {
                FileHandler.appendLine(logFile, line);
            } catch (Exception e) {
                // Logging must never crash the app; surface to stderr instead.
                System.err.println("Failed to write activity log: " + e.getMessage());
            }
        });
    }

    /** Flushes pending log writes and shuts the background thread down cleanly. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
