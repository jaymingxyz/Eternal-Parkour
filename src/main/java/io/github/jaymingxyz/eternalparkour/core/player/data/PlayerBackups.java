package io.github.jaymingxyz.eternalparkour.core.player.data;

import com.google.gson.JsonParser;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crash-safe storage of {@link PlayerBackup}s in {@code backups/<uuid>.json}.
 *
 * <p>A backup is written when a player joins block or elytra parkour and deleted once everything has been
 * given back. A backup that still exists therefore means the player never got their things back, usually
 * because the server stopped without shutting down cleanly. Such a backup is restored automatically the
 * next time the player joins.</p>
 *
 * <p>All file access runs on the {@link DataIO} thread, in order, so a backup is never read while it is
 * still being written or deleted.</p>
 */
public final class PlayerBackups {

    /**
     * Backups read while players log in, waiting for {@link #restoreOnJoin(Player)}.
     */
    private static final Map<UUID, PlayerBackup> PRELOADED = new ConcurrentHashMap<>();

    private PlayerBackups() {
    }

    /**
     * Writes a backup for a player. An existing backup was never restored, so it is kept under
     * another name instead of being overwritten. Must be called on the main thread.
     *
     * @param uuid   The player's UUID.
     * @param name   The player's name, for logging.
     * @param backup The backup.
     */
    public static void save(@NotNull UUID uuid, @NotNull String name, @NotNull PlayerBackup backup) {
        String json = backup.toJson().toString(); // items are serialized on the main thread

        DataIO.run(() -> {
            try {
                Path target = file(uuid);
                Files.createDirectories(target.getParent());

                if (Files.exists(target)) {
                    Path kept = target.resolveSibling(target.getFileName() + ".unrecovered-" + System.currentTimeMillis());
                    Files.move(target, kept);
                    EternalParkour.logging().warn("%s had a parkour backup that was never restored. It was kept as backups/%s."
                            .formatted(name, kept.getFileName()));
                }

                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Files.writeString(temp, json, StandardCharsets.UTF_8);
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException ex) {
                EternalParkour.logging().stack("Error while saving the parkour backup of %s".formatted(name), ex);
            }
        });
    }

    /**
     * Deletes a player's backup, once everything in it has been given back.
     *
     * @param uuid The player's UUID.
     */
    public static void delete(@NotNull UUID uuid) {
        DataIO.run(() -> {
            try {
                Files.deleteIfExists(file(uuid));
            } catch (IOException ex) {
                EternalParkour.logging().stack("Error while deleting the parkour backup of %s".formatted(uuid), ex);
            }
        });
    }

    /**
     * Reads a player's backup. Blocking: only call it on the I/O thread.
     *
     * @param uuid The player's UUID.
     * @return The backup, or null if there is none or it can't be read.
     */
    static @Nullable PlayerBackup read(@NotNull UUID uuid) {
        Path path = file(uuid);
        if (!Files.exists(path)) {
            return null;
        }

        try {
            return PlayerBackup.fromJson(JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (Exception ex) {
            EternalParkour.logging().stack("The parkour backup of %s could not be read; it was left in backups/ for manual recovery".formatted(uuid), ex);
            return null;
        }
    }

    /**
     * Loads a player's backup, if they have one, while they log in. Called from the async pre-login event.
     *
     * @param uuid The player's UUID.
     */
    public static void preload(@NotNull UUID uuid) {
        PlayerBackup backup = DataIO.call(() -> read(uuid));

        if (backup != null) {
            PRELOADED.put(uuid, backup);
        }
    }

    /**
     * Drops a preloaded backup that was never used, e.g. when the login was cancelled.
     *
     * @param uuid The player's UUID.
     */
    public static void forget(@NotNull UUID uuid) {
        PRELOADED.remove(uuid);
    }

    /**
     * Restores a backup left behind by a crash: location, gamemode, hunger, flight, effects and inventory.
     * Called when a player joins, before they can be put into parkour. Must be called on the main thread.
     *
     * @param player The player who joined.
     * @return True when the player is being sent back to the location in the backup.
     */
    public static boolean restoreOnJoin(@NotNull Player player) {
        PlayerBackup backup = PRELOADED.remove(player.getUniqueId());
        if (backup == null) {
            return false;
        }

        backup.applyState(player);
        ParkourItems.strip(player);

        Location location = backup.getLocation();
        if (location != null) {
            player.teleportAsync(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }

        delete(player.getUniqueId());

        EternalParkour.logging().info("Restored the parkour backup of %s, which the server didn't get to give back before it stopped."
                .formatted(player.getName()));
        player.sendMessage(Strings.component(EternalParkour.PREFIX + "Your inventory and position from before the server stopped have been restored."));
        return location != null;
    }

    /**
     * Loads a backup for {@code /ep recoverinventory}: the current backup, or else an Infinite Parkour
     * inventory backup. The callback runs on the main thread.
     *
     * @param player   The player.
     * @param callback Receives the backup, or null if there is none.
     */
    public static void loadForRecovery(@NotNull Player player, @NotNull java.util.function.Consumer<@Nullable PlayerBackup> callback) {
        UUID uuid = player.getUniqueId();
        int size = player.getInventory().getSize();

        DataIO.run(() -> {
            PlayerBackup backup = read(uuid);
            ItemStack[] legacy = backup == null ? readLegacyItems(uuid, size) : null;

            DataIO.sync(() -> {
                if (backup != null) {
                    callback.accept(backup);
                } else if (legacy != null) {
                    callback.accept(PlayerBackup.itemsOnly(player, legacy));
                } else {
                    callback.accept(null);
                }
            });
        });
    }

    /**
     * Deletes whichever backup {@link #loadForRecovery} found, after it has been restored.
     *
     * @param uuid The player's UUID.
     */
    public static void deleteAfterRecovery(@NotNull UUID uuid) {
        delete(uuid);
        DataIO.run(() -> {
            try {
                Files.deleteIfExists(legacyFile(uuid));
            } catch (IOException ex) {
                EternalParkour.logging().stack("Error while deleting the old inventory backup of %s".formatted(uuid), ex);
            }
        });
    }

    // Infinite Parkour backups: a Java-serialized Map<Integer, ItemStack> of slot to item.
    @SuppressWarnings({"unchecked", "deprecation"})
    private static @Nullable ItemStack[] readLegacyItems(UUID uuid, int size) {
        Path path = legacyFile(uuid);
        if (!Files.exists(path)) {
            return null;
        }

        try (var stream = new org.bukkit.util.io.BukkitObjectInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            stream.setObjectInputFilter(LegacySerialization.ITEM_FILTER);

            Map<Integer, ItemStack> items = (Map<Integer, ItemStack>) stream.readObject();
            ItemStack[] result = new ItemStack[size];
            items.forEach((slot, item) -> {
                if (slot >= 0 && slot < result.length) {
                    result[slot] = item;
                }
            });
            return result;
        } catch (Exception ex) {
            EternalParkour.logging().stack("Error while reading the old inventory backup of %s".formatted(uuid), ex);
            return null;
        }
    }

    private static Path file(UUID uuid) {
        return EternalParkour.getInFolder("backups/%s.json".formatted(uuid)).toPath();
    }

    private static Path legacyFile(UUID uuid) {
        return EternalParkour.getInFolder("inventories/%s".formatted(uuid)).toPath();
    }
}
