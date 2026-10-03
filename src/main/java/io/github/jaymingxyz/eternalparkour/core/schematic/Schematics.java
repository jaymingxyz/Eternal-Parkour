package io.github.jaymingxyz.eternalparkour.core.schematic;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stores schematics, so they don't have to be read every time.
 */
public class Schematics {

    private static final String[] SPAWN_SCHEMATICS = new String[]{
            "spawn-island", "spawn-island-duels"
    };
    private static final File FOLDER = EternalParkour.getInFolder("schematics");

    /**
     * Reads all files.
     */
    public static void init() {
        Task.create(EternalParkour.getPlugin()).async().execute(() -> {
            if (!FOLDER.exists()) {
                FOLDER.mkdirs();
            }

            File[] files = FOLDER.listFiles((dir, name) -> name.contains("parkour-") || name.contains("spawn-island"));

            if (files == null || files.length == 0) {
                download();
                init();
                return;
            }
          
            try {
                io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematics.addFromFiles(EternalParkour.getPlugin(), files);
            } catch (IOException | ClassNotFoundException ex) {
                EternalParkour.logging().stack("Error while trying to load schematics", ex);
            }
        }).run();
    }

    private static void download() {
        EternalParkour.log("Downloading schematics");

        List<String> schematics = new ArrayList<>();
        schematics.addAll(Arrays.asList(SPAWN_SCHEMATICS));
        schematics.addAll(Config.SCHEMATICS.getChildren("difficulty", false).stream().map("parkour-%s"::formatted).toList());
        schematics.forEach(file -> EternalParkour.getPlugin().saveResource("schematics/%s".formatted(file), true));
    }
}