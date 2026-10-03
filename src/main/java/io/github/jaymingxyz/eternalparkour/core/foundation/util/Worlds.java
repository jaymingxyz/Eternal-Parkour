package io.github.jaymingxyz.eternalparkour.core.foundation.util;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Creation and deletion of the plugin's void worlds.
 *
 * <p>Since Paper 26.1, extra worlds are stored under {@code <level>/dimensions/<namespace>/<key>/} instead of
 * {@code <server root>/<name>/}. Deletion therefore always uses {@link World#getWorldPath()}. That path is
 * remembered in the plugin's data folder so a world left behind by a crash can be deleted on the next start,
 * before it is loaded.</p>
 */
public final class Worlds {

    private Worlds() {
    }

    /**
     * Creates (or loads) a void world with parkour-friendly rules.
     *
     * @param plugin The owning plugin, used for logging and to remember the world path.
     * @param name   The world name.
     * @param wipe   Whether an existing copy of the world should be deleted first.
     * @return The world, or null if creation failed.
     */
    public static @Nullable World createVoidWorld(@NotNull Plugin plugin, @NotNull String name, boolean wipe) {
        Logger logger = plugin.getLogger();
        Set<Path> candidates = new LinkedHashSet<>();

        World existing = Bukkit.getWorld(name);
        if (existing != null) {
            logger.warning("World '%s' was already loaded by something else; unloading it so the void generator can be applied.".formatted(name));
            candidates.add(existing.getWorldPath());
            if (!Bukkit.unloadWorld(existing, false)) {
                logger.severe("Could not unload world '%s'. Another plugin is keeping it loaded; it may NOT be void.".formatted(name));
            }
        }

        if (wipe) {
            Path remembered = readRememberedPath(plugin, name);
            if (remembered != null) {
                candidates.add(remembered);
            }
            candidates.addAll(defaultLocations(name));

            for (Path candidate : candidates) {
                delete(plugin, name, candidate);
            }
        }

        World world;
        try {
            world = new WorldCreator(name)
                    .generator(VoidGenerator.getGenerator())
                    .type(WorldType.NORMAL)
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, "Error while creating world '%s'. Stop the server, delete the world folder and try again.".formatted(name), ex);
            return null;
        }

        if (world == null) {
            logger.severe("World '%s' is null after creation.".formatted(name));
            return null;
        }

        rememberPath(plugin, name, world.getWorldPath());
        applyRules(world);
        warnIfNotVoid(plugin, world);

        return world;
    }

    /**
     * Unloads the world without saving and deletes its folder.
     *
     * @param plugin The owning plugin.
     * @param world  The world. May be null, in which case only the remembered path is deleted.
     * @param name   The world name.
     */
    public static void deleteWorld(@NotNull Plugin plugin, @Nullable World world, @NotNull String name) {
        Path path = world != null ? world.getWorldPath() : readRememberedPath(plugin, name);

        if (world != null) {
            world.getPlayers().forEach(player -> {
                World fallback = Bukkit.getWorlds().stream().filter(other -> !other.equals(world)).findFirst().orElse(null);
                if (fallback != null) {
                    player.teleport(fallback.getSpawnLocation());
                }
            });
            if (!Bukkit.unloadWorld(world, false)) {
                plugin.getLogger().warning("Could not unload world '%s' before deleting it.".formatted(name));
            }
        }

        if (path != null) {
            delete(plugin, name, path);
        }
    }

    private static void applyRules(World world) {
        world.setGameRule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.BLOCK_DROPS, false);
        world.setGameRule(GameRules.ADVANCE_TIME, false);
        world.setGameRule(GameRules.ADVANCE_WEATHER, false);
        world.setGameRule(GameRules.LOG_ADMIN_COMMANDS, false);
        world.setGameRule(GameRules.KEEP_INVENTORY, true);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        world.setGameRule(GameRules.RANDOM_TICK_SPEED, 0);

        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(10_000_000);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setClearWeatherDuration(1_000_000);
        world.setAutoSave(false);
    }

    // Samples blocks around spawn. Anything solid means another generator filled the world.
    private static void warnIfNotVoid(Plugin plugin, World world) {
        try {
            Location spawn = world.getSpawnLocation();
            for (int y : new int[]{60, 64, 70, 100}) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        var type = world.getBlockAt(spawn.getBlockX() + dx, y, spawn.getBlockZ() + dz).getType();
                        if (!type.isAir()) {
                            plugin.getLogger().severe(("World '%s' is NOT void (found %s at y=%d). It was generated by another generator. "
                                    + "Stop the server, delete %s and start again.").formatted(world.getName(), type, y, world.getWorldPath()));
                            return;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not verify that world '%s' is void: %s".formatted(world.getName(), t));
        }
    }

    // Where a world with this name is stored on Paper 26.x, plus the pre-26.1 location for old installs.
    private static Set<Path> defaultLocations(String name) {
        Set<Path> paths = new LinkedHashSet<>();
        String key = name.toLowerCase(Locale.ROOT);
        try {
            Path level = Bukkit.getServer().getLevelDirectory();
            paths.add(level.resolve("dimensions").resolve("minecraft").resolve(key));
        } catch (Throwable ignored) {
            // not available, only use the legacy location
        }
        paths.add(Bukkit.getWorldContainer().toPath().resolve(name));
        return paths;
    }

    /**
     * Deletes a world folder, refusing anything that doesn't look like this world's own folder.
     */
    private static void delete(Plugin plugin, String name, Path path) {
        Path target = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(target) || !isSafeToDelete(name, target)) {
            return;
        }

        plugin.getLogger().info("Deleting world folder %s".formatted(target));

        for (int attempt = 1; attempt <= 2; attempt++) {
            boolean complete = true;
            try (Stream<Path> files = Files.walk(target)) {
                for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (IOException ex) {
                        complete = false;
                    }
                }
            } catch (IOException ex) {
                complete = false;
            }

            if (complete || !Files.exists(target)) {
                return;
            }

            // chunk IO may still hold a file lock for a moment after unloading, mostly on Windows
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        plugin.getLogger().severe("Could not fully delete %s. Stop the server and delete it manually.".formatted(target));
    }

    private static boolean isSafeToDelete(String name, Path target) {
        Path fileName = target.getFileName();
        if (fileName == null || !fileName.toString().equalsIgnoreCase(name)) {
            return false;
        }

        Path container = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        if (!target.startsWith(container) || target.equals(container)) {
            return false;
        }

        try {
            Path level = Bukkit.getServer().getLevelDirectory().toAbsolutePath().normalize();
            if (target.equals(level)) {
                return false;
            }
        } catch (Throwable ignored) {
            // older API, the container check above still applies
        }

        for (World loaded : Bukkit.getWorlds()) {
            if (loaded.getWorldPath().toAbsolutePath().normalize().equals(target)) {
                return false; // never delete a loaded world's folder
            }
        }
        return true;
    }

    private static Path rememberedPathFile(Plugin plugin, String name) {
        return plugin.getDataFolder().toPath().resolve(".world-paths").resolve(name.toLowerCase(Locale.ROOT) + ".txt");
    }

    private static @Nullable Path readRememberedPath(Plugin plugin, String name) {
        Path file = rememberedPathFile(plugin, name);
        try {
            if (Files.exists(file)) {
                String value = Files.readString(file, StandardCharsets.UTF_8).strip();
                return value.isEmpty() ? null : Path.of(value);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not read %s: %s".formatted(file, ex.getMessage()));
        }
        return null;
    }

    private static void rememberPath(Plugin plugin, String name, Path path) {
        Path file = rememberedPathFile(plugin, name);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, path.toAbsolutePath().normalize().toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write %s: %s".formatted(file, ex.getMessage()));
        }
    }
}
