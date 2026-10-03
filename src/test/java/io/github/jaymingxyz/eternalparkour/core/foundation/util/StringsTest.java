package io.github.jaymingxyz.eternalparkour.core.foundation.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringsTest {

    @Test
    void legacyCodesBecomeTags() {
        assertEquals("<reset><#ffd166>» Click", Strings.legacyToMiniMessage("§x§f§f§d§1§6§6» Click"));
        assertEquals("<reset><gray>Seed <reset><white>5<bold>!", Strings.legacyToMiniMessage("§7Seed §f5§l!"));
    }

    // colour() output used to throw in MiniMessage when it was coloured again
    @Test
    void colouredTextCanBeParsedAgain() {
        String coloured = Strings.colour("<#FFD166>» Click to play");
        Component component = Strings.component(coloured + " <gray>now");

        assertEquals("» Click to play now", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(TextColor.color(0xFFD166), firstColour(component));
    }

    @Test
    void mixedTextKeepsBothColours() {
        Component component = Strings.component("<gray>Seed set to " + Strings.colour("<white>42"));

        assertEquals("Seed set to 42", PlainTextComponentSerializer.plainText().serialize(component));
        assertEquals(NamedTextColor.GRAY, firstColour(component));
    }

    private static TextColor firstColour(Component component) {
        if (component.color() != null) {
            return component.color();
        }
        for (Component child : component.children()) {
            TextColor colour = firstColour(child);
            if (colour != null) {
                return colour;
            }
        }
        return null;
    }
}
