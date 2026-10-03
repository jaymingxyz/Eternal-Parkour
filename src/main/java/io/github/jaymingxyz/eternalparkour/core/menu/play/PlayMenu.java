package io.github.jaymingxyz.eternalparkour.core.menu.play;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.DynamicMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import org.bukkit.entity.Player;

/**
 * The menu where players can join modes
 */
public class PlayMenu extends DynamicMenu {

    public PlayMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "play.single.item")
                        .click(event -> Menus.SINGLE.open(event.getPlayer())),
                ParkourOption.SINGLE::mayPerform);

        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "other.iep")
                        .click(event -> event.getPlayer().performCommand("iep play")),
                player -> EternalParkour.isElytraEnabled());

        registerMainItem(1, 6, (player, user) -> Locales.getItem(player, "play.spectator.item")
                        .click(event -> Menus.SPECTATOR.open(event.getPlayer())),
                ParkourOption.SPECTATOR::mayPerform);

        registerMainItem(2, 0, (player, user) -> MenuStyle.close(MenuStyle.locale(player)),
                player -> true);
    }

    public void open(Player player) {
        display(player, MenuStyle.frame(new Menu(3, Locales.getString(player, "play.name")))
                .distributeRowsEvenly());
    }
}