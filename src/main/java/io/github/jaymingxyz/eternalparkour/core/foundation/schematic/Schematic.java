package io.github.jaymingxyz.eternalparkour.core.foundation.schematic;

import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.io.SchematicPaster;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.io.SchematicReader;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.io.SchematicWriter;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Main schematic handling class.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Schematic {

    /**
     * The version of this library instance.
     */
    public static final int VERSION = 1;
    private final File file;
    private final Map<Vector, BlockData> vectorBlockMap;
    /**
     * Constructor.
     *
     * @param file The file.
     */
    public Schematic(@NotNull File file, @NotNull Plugin plugin) throws IOException, ClassNotFoundException {
        this.file = file;
        this.vectorBlockMap = new SchematicReader().read(file, plugin);
    }

    /**
     * Pastes a schematic.
     *
     * @param location The smallest location.
     */
    public List<Block> paste(Location location) {
        return new SchematicPaster().paste(location, vectorBlockMap);
    }

    /**
     * Pastes a schematic at angles rotation.
     *
     * @param location The smallest location.
     * @param rotation The rotation where y = yaw in rad.
     */
    public List<Block> paste(Location location, double rotation) {
        return new SchematicPaster().paste(location, rotation, vectorBlockMap);
    }

    /**
     * @return The dimensions of this schematic.
     */
    public Vector getDimensions() {
        Set<Vector> offsets = vectorBlockMap.keySet();

        Vector min = offsets.stream().reduce((a, b) -> new Vector(min(a.getX(), b.getX()), min(a.getY(), b.getY()), min(a.getZ(), b.getZ()))).orElseThrow();
        Vector max = offsets.stream().reduce((a, b) -> new Vector(max(a.getX(), b.getX()), max(a.getY(), b.getY()), max(a.getZ(), b.getZ()))).orElseThrow();

        return max.subtract(min);
    }

    /**
     * @return True when this schematic contains unknown {@link BlockData}, false if it does.
     */
    public boolean hasUnknownMaterials() {
        return vectorBlockMap.containsValue(null);
    }

    /**
     * @return The map of vectors mapped to each {@link BlockData}.
     */
    public Map<Vector, BlockData> getVectorBlockMap() {
        return vectorBlockMap;
    }

    /**
     * @return The file.
     */
    public File getFile() {
        return file;
    }

    /**
     * Loads a schematic asynchronously.
     *
     * @param file The file.
     * @return A new {@link Schematic} instance.
     */
    public static Schematic load(File file, Plugin plugin) throws IOException, ClassNotFoundException {
        return new Schematic(file, plugin);
    }

    /**
     * Loads a schematic asynchronously.
     *
     * @param file The file.
     * @return A new {@link Schematic} instance.
     */
    public static Schematic load(String file, Plugin plugin) throws IOException, ClassNotFoundException {
        return new Schematic(new File(file), plugin);
    }

    /**
     * Saves the selection between the two locations asynchronously to file.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     */
    public static boolean save(String file, Location pos1, Location pos2, Plugin plugin) {
        return save(new File(file), pos1, pos2, plugin);
    }

    /**
     * Saves the selection between the two locations to file. The blocks are read now, so this must be
     * called on the main thread; the file is written on the I/O thread.
     *
     * @param file The file.
     * @param pos1 The first position.
     * @param pos2 The second position.
     * @return False if the selection contains no blocks, in which case nothing is saved.
     */
    public static boolean save(File file, Location pos1, Location pos2, Plugin plugin) {
        SchematicWriter writer = new SchematicWriter();
        SchematicWriter.Capture capture = writer.capture(pos1, pos2);

        if (capture.isEmpty()) {
            return false;
        }

        DataIO.run(() -> writer.write(file, capture, plugin));
        return true;
    }
}
