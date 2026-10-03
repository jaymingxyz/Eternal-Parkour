package io.github.jaymingxyz.eternalparkour.core.foundation.schematic.io;

import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Locations;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Schematic writing handler. Blocks are read with {@link #capture(Location, Location)} on the main thread;
 * the result is written to disk with {@link #write(File, Capture, Plugin)}, which can run on any thread.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class SchematicWriter {

    /**
     * The blocks of a selection, ready to be written.
     *
     * @param palette    Block data string to palette id.
     * @param offsetData Block offset (relative to the lowest block) to palette id.
     */
    public record Capture(Map<String, Integer> palette, Map<String, Integer> offsetData) {

        /**
         * @return True when the selection contained no blocks.
         */
        public boolean isEmpty() {
            return offsetData.isEmpty();
        }
    }

    /**
     * Reads all non-air blocks between two positions. Must be called on the main thread.
     *
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return The captured blocks.
     */
    public Capture capture(Location pos1, Location pos2) {
        List<Block> blocks = getBlocks(Locations.min(pos1, pos2), Locations.max(pos1, pos2));

        Map<String, Integer> palette = getPalette(blocks);
        Map<String, Integer> offsetData = getOffsetData(blocks, palette);

        return new Capture(palette, offsetData);
    }

    /**
     * Writes captured blocks to a file.
     *
     * @param file    The file.
     * @param capture The captured blocks.
     * @param plugin  The plugin, for logging.
     */
    public void write(File file, Capture capture, Plugin plugin) {
        try (ObjectOutputStream stream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            stream.writeObject(Schematic.VERSION);
            stream.writeObject(new HashMap<>(capture.palette()));
            stream.writeObject(new HashMap<>(capture.offsetData()));
            stream.flush();
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Error while saving schematic " + file.getName(), ex);
        }
    }

    // returns all blocks between the min location (minL) and max location (maxL)
    private List<Block> getBlocks(Location minL, Location maxL) {
        List<Block> blocks = new ArrayList<>();
        Location location = new Location(minL.getWorld() == null ? maxL.getWorld() : minL.getWorld(), 0, 0, 0);

        for (int x = minL.getBlockX(); x <= maxL.getBlockX(); x++) {
            for (int y = minL.getBlockY(); y <= maxL.getBlockY(); y++) {
                for (int z = minL.getBlockZ(); z <= maxL.getBlockZ(); z++) {
                    location.setX(x);
                    location.setY(y);
                    location.setZ(z);

                    Block block = location.getBlock();
                    if (block.getType().isAir()) {
                        continue;
                    }

                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    // returns block palette where ids are used instead of the full blockdata to reduce file space
    private Map<String, Integer> getPalette(List<Block> blocks) {
        Map<String, Integer> palette = new HashMap<>();

        blocks.stream()
            .map(block -> block.getBlockData().getAsString())
            .distinct()
            .forEach(datum -> palette.put(datum, palette.size()));

        return palette;
    }

    // returns the offset data, where each block offset (position relative to the lowest block)
    // is mapped to the palette id of the block at that position.
    private Map<String, Integer> getOffsetData(List<Block> blocks, Map<String, Integer> palette) {
        Map<String, Integer> offsetData = new HashMap<>();
        if (blocks.isEmpty()) {
            return offsetData;
        }

        Location min = blocks.stream().map(Block::getLocation).reduce(Locations::min).orElseThrow();

        for (Block block : blocks) {
            offsetData.put(block.getLocation().subtract(min).toVector().toString(),
                    palette.get(block.getBlockData().getAsString()));
        }

        return offsetData;
    }
}
