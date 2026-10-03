package io.github.jaymingxyz.eternalparkour.core.foundation.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * A single background thread for file and database I/O.
 *
 * <p>Using one thread keeps writes for the same data in order (for example a backup being written
 * and then deleted). While the plugin is disabling, Bukkit no longer accepts tasks, so work runs on
 * the calling thread instead, after everything already queued has finished (see {@link #flush()}).</p>
 */
public final class DataIO {

    private static Plugin plugin;
    private static ExecutorService executor;

    private DataIO() {
    }

    /**
     * Starts the I/O thread.
     *
     * @param pl The plugin.
     */
    public static synchronized void init(@NotNull Plugin pl) {
        plugin = pl;
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "EternalParkour-IO");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Runs a task on the I/O thread, or immediately on this thread while the plugin is disabling.
     *
     * @param task The task.
     */
    public static void run(@NotNull Runnable task) {
        ExecutorService current = executor;
        if (current == null || current.isShutdown() || plugin == null || !plugin.isEnabled()) {
            safely(task);
            return;
        }

        current.execute(() -> safely(task));
    }

    /**
     * Runs a task on the I/O thread and waits for its result, so it sees every write queued before it.
     * Never call this on the main thread. While the plugin is disabling, runs on this thread.
     *
     * @param task The task.
     * @return The task's result, or null if it failed.
     */
    public static <T> T call(@NotNull Supplier<T> task) {
        ExecutorService current = executor;
        if (current == null || current.isShutdown() || plugin == null || !plugin.isEnabled()) {
            return task.get();
        }

        try {
            return current.submit(task::get).get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | RejectedExecutionException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error during data I/O", ex);
            return null;
        }
    }

    /**
     * Runs a task on the main thread. Used to hand results of I/O back to game code.
     *
     * @param task The task.
     */
    public static void sync(@NotNull Runnable task) {
        if (plugin != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, task);
        } else {
            task.run();
        }
    }

    /**
     * Waits for all queued I/O to finish and stops the I/O thread. Called while disabling.
     */
    public static synchronized void flush() {
        if (executor == null) {
            return;
        }

        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().severe("Pending data writes did not finish within 30 seconds; some data may not have been saved.");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static void safely(Runnable task) {
        try {
            task.run();
        } catch (Throwable t) {
            if (plugin != null) {
                plugin.getLogger().log(Level.SEVERE, "Error during data I/O", t);
            }
        }
    }
}
