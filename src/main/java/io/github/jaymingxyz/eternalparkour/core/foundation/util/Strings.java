package io.github.jaymingxyz.eternalparkour.core.foundation.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.Ticks;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Strings {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .extractUrls()
            .hexColors()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .useUnusualXRepeatedCharacterHexFormat() // spigot makes me sad :(
            .build();

    /**
     * Colours a list of strings using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param strings The list of strings
     * @return the coloured list of strings
     */
    public static List<String> colour(@NotNull List<String> strings) {
        return strings.stream().map(Strings::colour).collect(Collectors.toList());
    }

    /**
     * Colours an array of strings using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param strings The array of strings
     * @return the array of strings, but coloured
     */
    @NotNull
    public static String[] colour(@NotNull String... strings) {
        String[] coloured = new String[strings.length];

        int index = 0;
        for (String string : strings) {
            coloured[index] = colour(string);
            index++;
        }

        return coloured;
    }

    /**
     * Colours a string using {@link MiniMessage} and {@link LegacyComponentSerializer}
     *
     * @param string The string
     * @return the coloured string
     */
    @NotNull
    public static String colour(@NotNull String string) {
        return LEGACY_COMPONENT_SERIALIZER.serialize(component(string));
    }

    /**
     * Parses a {@link MiniMessage} string. Only use this for trusted text (configs, locales), never for
     * player input: player input must be inserted with an unparsed placeholder or {@link Component#text(String)}.
     *
     * @param string The MiniMessage string
     * @return the component
     */
    @NotNull
    public static Component component(@NotNull String string) {
        // Some text mixes MiniMessage tags with values that already went through colour(). MiniMessage
        // refuses legacy codes, so turn them into tags first.
        if (string.indexOf(LegacyComponentSerializer.SECTION_CHAR) >= 0) {
            string = legacyToMiniMessage(string);
        }
        return MINI_MESSAGE.deserialize(string);
    }

    private static final Pattern LEGACY_HEX = Pattern.compile("§x((?:§[0-9a-fA-F]){6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("§([0-9a-fk-orA-FK-OR])");
    private static final String LEGACY_COLOURS = "0123456789abcdef";
    private static final String[] COLOUR_TAGS = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red",
            "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};

    /**
     * Rewrites legacy section sign codes as MiniMessage tags. A colour code resets the formatting before
     * it, like it does in the legacy format.
     *
     * @param string Text with legacy codes, possibly mixed with MiniMessage tags.
     * @return The text with only MiniMessage tags.
     */
    @NotNull
    static String legacyToMiniMessage(@NotNull String string) {
        String hex = LEGACY_HEX.matcher(string)
                .replaceAll(match -> "<reset><#" + match.group(1).replace("§", "") + ">");

        return LEGACY_CODE.matcher(hex).replaceAll(match -> {
            char code = Character.toLowerCase(match.group(1).charAt(0));
            int colour = LEGACY_COLOURS.indexOf(code);
            if (colour >= 0) {
                return "<reset><" + COLOUR_TAGS[colour] + ">";
            }
            return switch (code) {
                case 'k' -> "<obfuscated>";
                case 'l' -> "<bold>";
                case 'm' -> "<strikethrough>";
                case 'n' -> "<underlined>";
                case 'o' -> "<italic>";
                default -> "<reset>";
            };
        });
    }

    /**
     * Converts a legacy (section sign) string, as produced by {@link #colour(String)}, back to a component.
     *
     * @param legacy The legacy string
     * @return the component
     */
    @NotNull
    public static Component fromLegacy(@NotNull String legacy) {
        return LEGACY_COMPONENT_SERIALIZER.deserialize(legacy);
    }

    /**
     * Parses a {@link MiniMessage} string for use as an item name or lore line.
     * Item text is italic by default in vanilla; this turns that off unless the text sets it explicitly.
     *
     * @param string The MiniMessage string
     * @return the component
     */
    @NotNull
    public static Component itemText(@NotNull String string) {
        return component(string).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    /**
     * Shows a title made of {@link MiniMessage} strings.
     *
     * @param player   The player
     * @param title    The title
     * @param subtitle The subtitle
     * @param fadeIn   Fade-in time in ticks
     * @param stay     Stay time in ticks
     * @param fadeOut  Fade-out time in ticks
     */
    public static void showTitle(@NotNull Player player, @NotNull String title, @NotNull String subtitle,
                                 int fadeIn, int stay, int fadeOut) {
        player.showTitle(Title.title(component(title), component(subtitle),
                Title.Times.times(Ticks.duration(fadeIn), Ticks.duration(stay), Ticks.duration(fadeOut))));
    }

    /**
     * Removes all {@link MiniMessage} tags from a string.
     *
     * @param string The MiniMessage string
     * @return the string without tags
     */
    @NotNull
    public static String stripTags(@NotNull String string) {
        return MINI_MESSAGE.stripTags(string);
    }

    /**
     * Gets the closest matching string
     *
     * @param source  The source string
     * @param strings Strings which will be compared to this string
     * @return the closest matching string from parameter strings
     */
    public static String getClosestMatching(String source, List<String> strings) {
        int min = Integer.MAX_VALUE;
        String closest = "";

        for (String string : strings) {
            int distance = getLevenshteinDistance(source, string);
            if (distance < min) {
                min = distance;
                closest = string;
            }
        }
        return closest;
    }

    /**
     * Gets the levenshtein distance between two strings
     * Source: <a href="https://www.stephenenright.com/java-levenshtein-distance">https://www.stephenenright.com/java-levenshtein-distance</a>
     *
     * @param source The source string
     * @param other  The other string
     * @return the distance required
     */
    public static int getLevenshteinDistance(String source, String other) {
        int sourceLength = source.length();
        int otherLength = other.length();

        int[][] minDistanceMatrix = new int[sourceLength + 1][otherLength + 1];  // init the minimum distance matrix and add one to account for default values
        minDistanceMatrix[0][0] = 0;

        for (int row = 1; row <= sourceLength; row++) { // enter default edit values for the source string (in rows)
            minDistanceMatrix[row][0] = row;
        }

        for (int col = 1; col <= otherLength; col++) { // enter default edit values for the other string (in cols)
            minDistanceMatrix[0][col] = col;
        }

        for (int row = 1; row <= sourceLength; row++) {
            for (int col = 1; col <= otherLength; col++) { // go through every value and get the min value
                minDistanceMatrix[row][col] = getMinLevenshteinCost(source, other, minDistanceMatrix, row, col);
            }
        }

        return minDistanceMatrix[sourceLength][otherLength]; // get the last value
    }

    private static int getMinLevenshteinCost(String source, String other, int[][] minDistanceMatrix, int row, int col) {
        int insertion = minDistanceMatrix[row][col - 1] + 1;
        int deletion = minDistanceMatrix[row - 1][col] + 1;
        int substition = minDistanceMatrix[row - 1][col - 1];

        if (source.charAt(row - 1) != other.charAt(col - 1)) { // if the letters are the same skip adding a cost
            substition += 1;
        }

        return Numbers.min(insertion, deletion, substition);
    }
}