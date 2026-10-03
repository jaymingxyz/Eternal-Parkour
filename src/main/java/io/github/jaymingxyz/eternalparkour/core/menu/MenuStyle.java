package io.github.jaymingxyz.eternalparkour.core.menu;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.MenuClickEvent;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The look every menu shares:
 * <ul>
 *     <li>A background without tooltips, and a darker bar along the bottom holding the navigation: back or
 *     close in the middle, page buttons in the corners.</li>
 *     <li>Lists are 5 rows: up to 21 entries per page inside a border, a short row centred.</li>
 *     <li>Items end their lore with a hint saying what a click does.</li>
 * </ul>
 */
public final class MenuStyle {

    public static final Material BACKGROUND = Material.GRAY_STAINED_GLASS_PANE;
    public static final Material BAR = Material.BLACK_STAINED_GLASS_PANE;

    /**
     * Rows of a list menu: a top border, three rows of entries and the bar.
     */
    public static final int LIST_ROWS = 5;

    private MenuStyle() {
    }

    /**
     * Adds the background and the bar along the bottom.
     */
    public static <T extends Menu> T frame(@NotNull T menu) {
        menu.fillBackground(BACKGROUND);
        menu.fillRow(menu.getRows() - 1, BAR);
        return menu;
    }

    /**
     * @return The middle slot of the bottom bar, where back or close goes.
     */
    public static int center(@NotNull Menu menu) {
        return (menu.getRows() - 1) * 9 + 4;
    }

    /**
     * Adds a hint such as "» Click to play" to the end of the lore, after an empty line.
     *
     * @param item The item. Clone it first when it's shared.
     * @param hint The hint, or null or empty for none.
     * @return The item.
     */
    public static Item hint(@NotNull Item item, @Nullable String hint) {
        if (hint == null || hint.isEmpty()) {
            return item;
        }

        List<String> lore = new ArrayList<>(item.getLore());
        if (!lore.isEmpty()) {
            lore.add("");
        }
        lore.add(hint);
        return item.lore(lore);
    }

    /**
     * @param locale The locale.
     * @param type   The hint under {@code other.hints}, like {@code play} or {@code change}.
     * @return The hint text.
     */
    public static String hint(@NotNull String locale, @NotNull String type) {
        return Locales.getString(locale, "other.hints." + type);
    }

    /**
     * @return The locale the player sees menus in.
     */
    public static String locale(@NotNull Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        return user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;
    }

    /**
     * @return A back button, for menus opened from another menu.
     */
    public static MenuItem back(@NotNull String locale, @NotNull Consumer<MenuClickEvent> action) {
        return Locales.getItem(locale, "other.back").click(action);
    }

    /**
     * @return A close button, for menus with nothing to go back to.
     */
    public static MenuItem close(@NotNull String locale) {
        return Locales.getItem(locale, "other.close").click(event -> event.getPlayer().closeInventory());
    }

    /**
     * A framed list: entries inside the border, page buttons in the corners of the bottom bar and a
     * placeholder when there is nothing to show.
     *
     * @param title  The title.
     * @param locale The locale of the buttons.
     * @return The menu.
     */
    public static PagedMenu list(@NotNull String title, @NotNull String locale) {
        return list(title,
                Locales.getItem(locale, "other.previous_page"),
                Locales.getItem(locale, "other.next_page"),
                Locales.getItem(locale, "other.empty"));
    }

    /**
     * A framed list with buttons from another set of locales.
     *
     * @see #list(String, String)
     */
    public static PagedMenu list(@NotNull String title, @NotNull Item previousPage, @NotNull Item nextPage, @Nullable Item empty) {
        PagedMenu menu = frame(new PagedMenu(LIST_ROWS, title));
        int bar = (LIST_ROWS - 1) * 9;

        return menu.displayArea(1, LIST_ROWS - 2)
                .pageButtons(bar, bar + 8, previousPage, nextPage)
                .whenEmpty(empty);
    }
}
