package io.github.jaymingxyz.eternalparkour.core.migration;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Imports data from the plugins Eternal Parkour replaces, without modifying the originals:
 * <ul>
 *     <li>{@code plugins/IP} (Infinite Parkour / Infinite Parkour Reborn) into the data folder root</li>
 *     <li>{@code plugins/IPPlus} into {@code plus/}</li>
 *     <li>{@code plugins/IEP} into {@code elytra/}</li>
 * </ul>
 * Only files that don't exist yet are copied. Each source is imported once; the imported sources are
 * recorded in {@value #MARKER} so files deleted afterwards aren't copied back on the next start.
 */
public final class LegacyDataMigrator {

    private static final String MARKER = ".legacy-imports";

    private LegacyDataMigrator() {
    }

    public static void migrate(EternalParkour plugin) {
        Path dataFolder = plugin.getDataFolder().toPath();
        Path plugins = dataFolder.getParent();
        if (plugins == null) {
            return;
        }

        Path marker = dataFolder.resolve(MARKER);
        Set<String> imported = readMarker(plugin, marker);

        int copied = 0;
        copied += importOnce(plugin, imported, "IP", plugins.resolve("IP"), dataFolder);
        copied += importOnce(plugin, imported, "IPPlus", plugins.resolve("IPPlus"), dataFolder.resolve("plus"));
        copied += importOnce(plugin, imported, "IEP", plugins.resolve("IEP"), dataFolder.resolve("elytra"));

        writeMarker(plugin, marker, imported);

        if (copied > 0) {
            plugin.getLogger().info("Imported " + copied + " legacy IP/IPPlus/IEP data files; the original folders were left untouched.");
        }
    }

    private static int importOnce(EternalParkour plugin, Set<String> imported, String name, Path source, Path target) {
        if (imported.contains(name) || !Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        int copied = copyMissing(plugin, source, target);
        imported.add(name);
        return copied;
    }

    private static Set<String> readMarker(EternalParkour plugin, Path marker) {
        Set<String> imported = new HashSet<>();
        if (!Files.exists(marker)) {
            return imported;
        }

        try {
            for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) {
                    imported.add(line.strip());
                }
            }
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read " + marker + ": " + exception.getMessage());
        }
        return imported;
    }

    private static void writeMarker(EternalParkour plugin, Path marker, Set<String> imported) {
        if (imported.isEmpty()) {
            return;
        }

        try {
            Files.createDirectories(marker.getParent());
            Files.write(marker, imported, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not write " + marker + ": " + exception.getMessage());
        }
    }

    private static int copyMissing(EternalParkour plugin, Path source, Path target) {
        AtomicInteger copied = new AtomicInteger();
        try (Stream<Path> paths = Files.walk(source)) {
            paths.filter(path -> !Files.isSymbolicLink(path)).forEach(path -> {
                Path relative = source.relativize(path);
                Path destination = target.resolve(relative);
                try {
                    if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination);
                    } else if (Files.notExists(destination, LinkOption.NOFOLLOW_LINKS)) {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination, StandardCopyOption.COPY_ATTRIBUTES);
                        copied.incrementAndGet();
                    }
                } catch (IOException exception) {
                    plugin.getLogger().warning("Could not import legacy file " + path + ": " + exception.getMessage());
                }
            });
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not inspect legacy data folder " + source + ": " + exception.getMessage());
        }
        return copied.get();
    }
}
