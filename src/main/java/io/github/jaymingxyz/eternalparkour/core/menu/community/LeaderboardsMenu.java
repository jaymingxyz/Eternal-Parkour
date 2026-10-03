package io.github.jaymingxyz.eternalparkour.core.menu.community;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Leaderboards menu
 */
public class LeaderboardsMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        PagedMenu menu = MenuStyle.list(Locales.getString(player, "%s.name".formatted(ParkourOption.LEADERBOARDS.path)), locale);

        Mode latest = null;
        List<MenuItem> items = new ArrayList<>();
        for (Mode mode : Registry.getModes()) {
            Leaderboard leaderboard = mode.getLeaderboard();
            Item item = mode.getItem(locale);

            if (leaderboard == null || item == null) {
                continue;
            }

            items.add(MenuStyle.hint(item.clone(), MenuStyle.hint(locale, "view")).click(event -> Menus.SINGLE_LEADERBOARD.open(player, mode, leaderboard.sort)));
            latest = mode;
        }

        if (items.size() == 1) {
            Menus.SINGLE_LEADERBOARD.open(player, latest, Leaderboard.Sort.SCORE);
            return;
        }

        menu
                .addToDisplay(items)
                .item(MenuStyle.center(menu), MenuStyle.back(locale, event -> Menus.COMMUNITY.open(event.getPlayer())))
                .open(player);
    }
}