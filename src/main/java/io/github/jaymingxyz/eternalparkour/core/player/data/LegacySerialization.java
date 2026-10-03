package io.github.jaymingxyz.eternalparkour.core.player.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.util.Base64;

/**
 * Reading of the Java-serialized files written by Infinite Parkour. These are never written anymore.
 *
 * <p>Java deserialization can run arbitrary code when fed a crafted file, so every read is limited
 * to an allow-list of classes that these files can actually contain.</p>
 */
public final class LegacySerialization {

    /**
     * Allows what Bukkit uses to serialize items and vectors (plain Java types, Guava collections,
     * Bukkit's serialization wrapper) and nothing else.
     */
    public static final ObjectInputFilter ITEM_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=64;maxrefs=1000000;java.lang.*;java.util.*;com.google.common.collect.*;org.bukkit.**;!*");

    /**
     * Allows what schematic files contain: a version number, a palette and block offsets.
     */
    public static final ObjectInputFilter SCHEMATIC_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=8;java.lang.Integer;java.lang.Number;java.lang.String;java.util.HashMap;java.util.LinkedHashMap;java.util.Map$Entry;!*");

    private LegacySerialization() {
    }

    /**
     * Reads a Base64 string written by Bukkit's object output stream.
     *
     * @param base64 The string.
     * @param type   The expected type.
     * @return The object.
     */
    @SuppressWarnings("deprecation")
    public static <T> T readBase64(String base64, Class<T> type) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getMimeDecoder().decode(base64);

        try (var stream = new org.bukkit.util.io.BukkitObjectInputStream(new ByteArrayInputStream(bytes))) {
            stream.setObjectInputFilter(ITEM_FILTER);
            return type.cast(stream.readObject());
        }
    }
}
