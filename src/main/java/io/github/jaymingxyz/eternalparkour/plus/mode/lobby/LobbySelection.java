package io.github.jaymingxyz.eternalparkour.plus.mode.lobby;

import com.google.gson.annotations.Expose;
import io.github.jaymingxyz.eternalparkour.core.player.data.LegacySerialization;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A lobby area, stored as JSON in {@code plus/worlds/<world>.json}.
 */
public class LobbySelection {

    @Expose
    public double[] min;
    @Expose
    public double[] max;

    /**
     * Infinite Parkour stored the corners as Base64 Java-serialized vectors. Only read, never written.
     */
    @Expose(serialize = false)
    public String pos1;
    @Expose(serialize = false)
    public String pos2;

    private transient BoundingBox bb;

    @SuppressWarnings("unused") // Gson
    private LobbySelection() {
    }

    public LobbySelection(BoundingBox bb) {
        this.bb = bb.clone();
        this.min = new double[]{bb.getMinX(), bb.getMinY(), bb.getMinZ()};
        this.max = new double[]{bb.getMaxX(), bb.getMaxY(), bb.getMaxZ()};
    }

    /**
     * Converts a selection read from JSON, in either the current or the Infinite Parkour format.
     *
     * @param read The selection as read by Gson.
     * @return The selection, or null if the stored data is incomplete.
     */
    public static @Nullable LobbySelection resolve(@Nullable LobbySelection read) throws IOException, ClassNotFoundException {
        if (read == null) {
            return null;
        }

        if (read.min != null && read.max != null && read.min.length == 3 && read.max.length == 3) {
            return new LobbySelection(new BoundingBox(read.min[0], read.min[1], read.min[2], read.max[0], read.max[1], read.max[2]));
        }

        if (read.pos1 != null && read.pos2 != null) {
            Vector one = LegacySerialization.readBase64(read.pos1, Vector.class);
            Vector two = LegacySerialization.readBase64(read.pos2, Vector.class);
            return new LobbySelection(BoundingBox.of(one, two));
        }

        return null;
    }

    /**
     * @return The area.
     */
    public BoundingBox getBb() {
        return bb;
    }
}
