package io.github.jaymingxyz.eternalparkour.core.world;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Worlds;

/**
 * The block-parkour world.
 */
public class World {

    private static String name;
    private static org.bukkit.World world;

    /**
     * Creates the parkour world. When {@code world.delete-on-reload} is enabled, any copy left over
     * from a previous run (including after a crash) is deleted first, so no ghost blocks remain.
     */
    public static void create() {
        name = Config.CONFIG.getString("world.name");

        if (!Config.CONFIG.getBoolean("joining")) {
            return;
        }

        EternalParkour.log("Creating parkour world %s".formatted(name));

        world = Worlds.createVoidWorld(EternalParkour.getPlugin(), name, Config.CONFIG.getBoolean("world.delete-on-reload"));

        if (world == null) {
            EternalParkour.logging().error("Parkour world '%s' could not be created. Block parkour will not work this session.".formatted(name));
        }
    }

    /**
     * Deletes the world, if {@code world.delete-on-reload} is enabled.
     */
    public static void delete() {
        if (!Config.CONFIG.getBoolean("world.delete-on-reload") || !Config.CONFIG.getBoolean("joining")) {
            return;
        }
        EternalParkour.log("Deleting parkour world");

        Worlds.deleteWorld(EternalParkour.getPlugin(), world, name);
        world = null;
    }

    /**
     * @return the name of the parkour world.
     */
    public static String getName() {
        return name;
    }

    /**
     * @return the Bukkit world wherein EternalParkour is currently active.
     */
    public static org.bukkit.World getWorld() {
        return world;
    }
}
