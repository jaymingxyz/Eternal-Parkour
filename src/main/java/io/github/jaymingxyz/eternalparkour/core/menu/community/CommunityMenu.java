package io.github.jaymingxyz.eternalparkour.core.menu.community;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.DynamicMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import org.bukkit.entity.Player;

/**
 * The menu for all community-related things
 */
public class CommunityMenu extends DynamicMenu {

    public CommunityMenu() {
        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "community.leaderboards.item").click(event -> Menus.LEADERBOARDS.open(event.getPlayer())), ParkourOption.LEADERBOARDS::mayPerform);
        registerMainItem(2, 10, (player, user) -> MenuStyle.close(MenuStyle.locale(player)), player -> true);
    }

    public void open(Player player) {
        display(player, MenuStyle.frame(new Menu(3, Locales.getString(player, "community.name")))
                .distributeRowsEvenly());
    }
}
