package io.github.jaymingxyz.eternalparkour.core.foundation.schematic;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Schematics {

    // Block parkour loads its schematics on another thread while elytra parkour loads its own on the main
    // thread, both under the same plugin.
    private static final Map<Plugin, Map<String, Schematic>> cache = new ConcurrentHashMap<>();

    public static void addFromFiles(@NotNull Plugin plugin, @NotNull File... files) throws IOException, ClassNotFoundException {
        Map<String, Schematic> current = cache.computeIfAbsent(plugin, key -> new ConcurrentHashMap<>());

        for (File file : files) {
            current.put(file.getName(), Schematic.load(file, plugin));
        }

        plugin.getLogger().info("Loaded all schematics!");
    }

    public static Schematic getSchematic(@NotNull Plugin plugin, @NotNull String schematicName) {
        return cache.getOrDefault(plugin, Map.of()).get(schematicName);
    }

    public static Set<String> getSchematicNames(@NotNull Plugin plugin) {
        return cache.getOrDefault(plugin, Map.of()).keySet();
    }

    public static Collection<Schematic> getSchematics(@NotNull Plugin plugin) {
        return cache.getOrDefault(plugin, Map.of()).values();
    }
}
