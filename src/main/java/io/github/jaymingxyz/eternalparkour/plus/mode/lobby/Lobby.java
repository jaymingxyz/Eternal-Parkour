package io.github.jaymingxyz.eternalparkour.plus.mode.lobby;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Numbers;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.IPP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Class for lobby modes
 */
public class Lobby {

    /**
     * The range from the edge of the selection, in order to ensure a safe spawn.
     */
    public static final int LOBBY_SAFE_RANGE = 10;

    /**
     * The minimum size of the axes of a selection.
     */
    public static final int MINIMUM_SIZE = 3 * LOBBY_SAFE_RANGE; // 30x30x30
    private static final Map<World, LobbySelection> selections = new HashMap<>();
    private static final Path FOLDER = IPP.getDataFolder().toPath().resolve("worlds");

    /**
     * Reads all lobby mode files in {@code plus/worlds}. Files are read on the I/O thread; the
     * selections are stored on the main thread.
     */
    public static void read() {
        DataIO.run(() -> {
            Map<String, LobbySelection> read = new HashMap<>();

            try {
                Files.createDirectories(FOLDER);

                try (Stream<Path> files = Files.list(FOLDER)) {
                    for (Path path : files.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                        String worldName = path.getFileName().toString().replaceFirst("\\.json$", "");

                        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                            LobbySelection selection = LobbySelection.resolve(EternalParkour.getGson().fromJson(reader, LobbySelection.class));

                            if (selection != null) {
                                read.put(worldName, selection);
                            }
                        } catch (Exception ex) {
                            IPP.logging().stack("Could not read lobby file %s".formatted(path.getFileName()), "delete that file and set up the lobby again", ex);
                        }
                    }
                }
            } catch (Exception ex) {
                IPP.logging().stack("Could not list files in the lobbies folder", ex);
            }

            DataIO.sync(() -> read.forEach((worldName, selection) -> {
                World world = Bukkit.getWorld(worldName);

                if (world != null) {
                    selections.put(world, selection);
                }
            }));
        });
    }

    /**
     * Saves lobby mode settings for a specific world in memory and in {@code plus/worlds/<world>.json}.
     * Must be called on the main thread.
     *
     * @param world     The world
     * @param selection The selection
     */
    public static void save(@NotNull World world, @NotNull BoundingBox selection) {
        LobbySelection sel = new LobbySelection(selection);
        selections.put(world, sel);

        String json = EternalParkour.getGson().toJson(sel);
        String worldName = world.getName();

        DataIO.run(() -> {
            try {
                Files.createDirectories(FOLDER);
                Files.writeString(FOLDER.resolve(worldName + ".json"), json, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                IPP.logging().stack("Error while trying to save lobby mode settings for world " + worldName, ex);
            }
        });
    }

    /**
     * Joins a player to the lobby mode in the world they are in, if there is one.
     *
     * @param session The session
     */
    public static void join(@NotNull Session session) {
        ParkourPlayer player = session.getPlayers().get(0);
        World world = player.player.getWorld();

        // set spawn block
        Location location = generateSpawn(world, session.generator);

        if (location == null) {
            return;
        }

        session.generator.island.blocks = List.of(location.getBlock());

        // the player spawn
        Location spawn = location.clone().add(0.5, 1, 0.5);

        spawn.setYaw(-90);

        session.generator.generateFirst(spawn, location);
        player.setup(spawn);
        session.generator.startTick();
    }

    /**
     * Generates a random spawn in the lobby selection in a specific world.
     * Sets the spawn block as well.
     *
     * @param world     The world.
     * @param generator The player's generator
     * @return the Location of the spawn block.
     */
    public static @Nullable Location generateSpawn(@NotNull World world, @NotNull ParkourGenerator generator) {
        LobbySelection selection = selections.get(world);

        if (selection == null) {
            return null;
        }

        BoundingBox bb = selection.getBb();
        Location min = bb.getMin().toLocation(world);
        Location max = bb.getMax().toLocation(world);

        generator.zone = new Location[]{min, max};

        // get random block in selection
        int x = Numbers.random(min.getBlockX() + LOBBY_SAFE_RANGE, max.getBlockX() - LOBBY_SAFE_RANGE);
        int y = Numbers.random(min.getBlockY() + LOBBY_SAFE_RANGE, max.getBlockY() - LOBBY_SAFE_RANGE);
        int z = Numbers.random(min.getBlockZ() + LOBBY_SAFE_RANGE, max.getBlockZ() - LOBBY_SAFE_RANGE);

        Location location = new Location(world, x, y, z);
        location.getBlock().setType(Material.SMOOTH_QUARTZ, false);

        return location;
    }

    public static Map<World, LobbySelection> getSelections() {
        return selections;
    }
}
