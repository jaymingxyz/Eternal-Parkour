package io.github.jaymingxyz.eternalparkour.core.api;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.style.Style;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.List;

/**
 * Registers stuff.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public final class Registry {

    // read from any thread (e.g. PlaceholderAPI), changed rarely
    private static final List<Mode> modes = new CopyOnWriteArrayList<>();
    private static final List<Style> styles = new CopyOnWriteArrayList<>();

    /**
     * Registers a {@link Mode}.
     *
     * @param mode The mode.
     */
    public static void register(@NotNull Mode mode) {
        replaceOrAdd(modes, mode, Mode::getName);

        EternalParkour.log("Registered mode %s".formatted(mode.getName()));
    }

    /**
     * Registers a {@link Style}. A style with the same name replaces the existing one, so reloading
     * the config updates styles instead of adding duplicates.
     *
     * @param style The style.
     */
    public static void register(@NotNull Style style) {
        replaceOrAdd(styles, style, Style::getName);

        EternalParkour.log("Registered style %s".formatted(style.getName()));
    }

    /**
     * @param name The mode name.
     * @return The {@link Mode} instance. May be null.
     */
    @Nullable
    public static Mode getMode(@NotNull String name) {
        return modes.stream()
                .filter(mode -> mode.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Nullable
    public static Style getStyle(@NotNull String name) {
        return styles.stream()
                .filter(style -> style.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public static List<Style> getStyles() {
        return styles;
    }

    public static List<Mode> getModes() {
        return modes;
    }

    private static <T> void replaceOrAdd(List<T> list, T value, Function<T, String> name) {
        String key = name.apply(value);

        for (int i = 0; i < list.size(); i++) {
            if (name.apply(list.get(i)).equals(key)) {
                list.set(i, value);
                return;
            }
        }
        list.add(value);
    }
}