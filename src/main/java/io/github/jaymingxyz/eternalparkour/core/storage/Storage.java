package io.github.jaymingxyz.eternalparkour.core.storage;

import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage of scores and player settings, either as JSON files on disk or in MySQL.
 *
 * <p>Methods that take or return data block while they read or write, and are called from the I/O thread
 * ({@link DataIO}) or during startup and shutdown. Player settings are loaded while the player logs in
 * ({@link #preload(UUID)}) and cached until they quit, so joining parkour never waits on storage.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Storage {

    /**
     * Settings of online players, keyed by UUID. Filled on login, updated on every save, cleared on quit.
     */
    private static final Map<UUID, Map<String, Object>> SETTINGS = new ConcurrentHashMap<>();

    /**
     * Prepares storage for a mode's leaderboard.
     */
    public static void init(String mode) {
        if (Option.SQL) {
            StorageSQL.init(mode);
        }
    }

    public static void close() {
        if (Option.SQL) {
            StorageSQL.close();
        }
    }

    /**
     * Reads scores. Blocking.
     *
     * @param mode The mode.
     * @return Map with all scores, unsorted.
     */
    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        if (Option.SQL) {
            return StorageSQL.readScores(mode);
        } else {
            return StorageDisk.readScores(mode);
        }
    }

    /**
     * Writes scores. Blocking.
     *
     * @param mode    The mode.
     * @param scores  All scores.
     * @param removed Players whose score was reset since the last write.
     */
    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores, @NotNull Collection<UUID> removed) {
        if (Option.SQL) {
            StorageSQL.writeScores(mode, scores, removed);
        } else {
            StorageDisk.writeScores(mode, scores);
        }
    }

    /**
     * Loads a player's settings into the cache. Blocking; called while the player logs in.
     *
     * @param uuid The player's UUID.
     */
    public static void preload(@NotNull UUID uuid) {
        SETTINGS.put(uuid, loadSettings(uuid));
    }

    /**
     * Removes a player's settings from the cache. Called when the player quits.
     *
     * @param uuid The player's UUID.
     */
    public static void forget(@NotNull UUID uuid) {
        SETTINGS.remove(uuid);
    }

    /**
     * Applies stored settings to a player. Uses the settings loaded at login; only if those are missing
     * (for example after a reload) are they read now, which blocks.
     *
     * @param player The player.
     */
    public static void readPlayer(@NotNull ParkourPlayer player) {
        Map<String, Object> settings = SETTINGS.computeIfAbsent(player.getUUID(), Storage::loadSettings);

        player.setSettings(settings);
    }

    /**
     * Saves a player's settings.
     *
     * @param player The player. Read on the calling thread, which must be the main thread.
     * @param async  Whether to write on the I/O thread.
     */
    public static void writePlayer(@NotNull ParkourPlayer player, boolean async) {
        UUID uuid = player.getUUID();
        Map<String, Object> settings = player.getSettings();
        SETTINGS.put(uuid, settings);

        Runnable write = () -> {
            if (Option.SQL) {
                StorageSQL.writePlayer(uuid, settings);
            } else {
                StorageDisk.writePlayer(uuid, settings);
            }
        };

        if (async) {
            DataIO.run(write);
        } else {
            write.run();
        }
    }

    private static Map<String, Object> loadSettings(UUID uuid) {
        return Option.SQL ? StorageSQL.readPlayer(uuid) : StorageDisk.readPlayer(uuid);
    }
}
