package io.github.jaymingxyz.eternalparkour.core.menu.settings;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class LangMenu {

    /**
     * Opens the language menu
     *
     * @param user The ParkourPlayer instance
     */
    public void open(ParkourPlayer user) {
        if (user == null) {
            return;
        }

        PagedMenu style = MenuStyle.list(Locales.getString(user.locale, "settings.lang.name"), user.locale);

        List<MenuItem> items = new ArrayList<>();
        for (String lang : Locales.locales.keySet()) {
            boolean current = lang.equals(user.locale);
            Item item = new Item(Material.PAPER, "<#238681><bold>" + Locales.getString(lang, "name")).glowing(current);

            items.add(MenuStyle.hint(item, MenuStyle.hint(user.locale, current ? "selected" : "select")).click(event -> {
                user.locale = lang;
                user._locale = lang;
                Menus.SETTINGS.open(event.getPlayer());
            }));
        }

        style.addToDisplay(items)
                .item(MenuStyle.center(style), MenuStyle.back(user.locale, event -> Menus.SETTINGS.open(event.getPlayer())))
                .open(user.player);
    }

}
