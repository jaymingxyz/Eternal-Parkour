package io.github.jaymingxyz.eternalparkour.core.menu.lobby;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.DynamicMenu;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.SliderItem;
import org.bukkit.entity.Player;

import java.util.List;

public class LobbyMenu extends DynamicMenu {

    public LobbyMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "lobby.player_management.item").click(event -> Menus.PLAYER_MANAGEMENT.open(player)), player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.PLAYER_MANAGEMENT.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(1, 1, (player, user) -> {
            if (user == null) {
                return null;
            }

            List<String> values = Locales.getStringList(user.locale, "lobby.visibility.values");

            return new SliderItem().initial(switch (user.session.getVisibility()) {
                case PUBLIC -> 0;
                case ID_ONLY -> 1;
                case PRIVATE -> 2;
            }).add(0, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.PUBLIC);
                }

                return true;
            }).add(1, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // id only
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.ID_ONLY);
                }

                return true;
            }).add(2, Locales.getItem(player, "lobby.visibility").modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // private
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.session.setVisibility(Session.Visibility.PRIVATE);
                }

                return true;
            });
        }, player -> {
            ParkourUser user = ParkourUser.getUser(player);

            return ParkourOption.VISIBILITY.mayPerform(player) && user instanceof ParkourPlayer && user.session.getPlayers().get(0) == user;
        });

        registerMainItem(2, 10, (player, user) -> MenuStyle.close(MenuStyle.locale(player)), player -> true);
    }

    /**
     * Opens the main menu.
     *
     * @param player The player to open the menu to
     */
    public void open(Player player) {
        display(player, MenuStyle.frame(new Menu(3, Locales.getString(player, "lobby.name")))
                .distributeRowsEvenly());
    }
}