package io.github.jaymingxyz.eternalparkour.plus.menu;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Cooldowns;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MultiplayerMenu {

    /**
     * Opens the multiplayer creation menu
     *
     * @param player The player
     */
    public static void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user != null ? user.locale : Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);

        List<MenuItem> items = new ArrayList<>();
        PagedMenu menu = MenuStyle.list(PlusLocales.getString(locale, "play.multi.name", false), locale);

        for (Mode mode : Registry.getModes()) {
            boolean permissions = Config.CONFIG.getBoolean("permissions.enabled") && !player.hasPermission("ip.gamemode.%s".formatted(mode.getName()));

            if (permissions || !(mode instanceof MultiMode) || mode.getItem(locale) == null) {
                continue;
            }

            items.add(MenuStyle.hint(mode.getItem(locale), MenuStyle.hint(locale, "create")).click(event -> {
                if (Cooldowns.canPerform(player, "switch gamemode", 5000)) {
                    mode.create(player);
                }
            }));
        }

        menu
                .addToDisplay(items)
                .item(MenuStyle.center(menu), MenuStyle.back(locale, event -> Menus.PLAY.open(event.getPlayer())))
                .open(player);
    }
}