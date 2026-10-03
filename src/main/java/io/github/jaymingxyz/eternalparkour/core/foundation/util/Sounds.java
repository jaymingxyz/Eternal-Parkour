package io.github.jaymingxyz.eternalparkour.core.foundation.util;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Sound lookups through the sound registry.
 */
public final class Sounds {

    private Sounds() {
    }

    /**
     * Resolves a sound by its key ({@code block.note_block.pling}, {@code minecraft:block.note_block.pling})
     * or by its legacy enum-style name ({@code BLOCK_NOTE_BLOCK_PLING}), which older configs use.
     *
     * @param name The sound name.
     * @return The sound, or null if no sound matches.
     */
    public static @Nullable Sound resolve(@NotNull String name) {
        String trimmed = name.strip();
        if (trimmed.isEmpty()) {
            return null;
        }

        NamespacedKey key = NamespacedKey.fromString(trimmed.toLowerCase(Locale.ROOT));
        if (key != null) {
            Sound sound = Registry.SOUND_EVENT.get(key);
            if (sound != null) {
                return sound;
            }
        }

        // Legacy names are the key path in upper case with dots replaced by underscores.
        String legacy = trimmed.toUpperCase(Locale.ROOT);
        for (Sound sound : Registry.SOUND_EVENT) {
            NamespacedKey soundKey = Registry.SOUND_EVENT.getKey(sound);
            if (soundKey != null && soundKey.getKey().toUpperCase(Locale.ROOT).replace('.', '_').equals(legacy)) {
                return sound;
            }
        }
        return null;
    }
}
