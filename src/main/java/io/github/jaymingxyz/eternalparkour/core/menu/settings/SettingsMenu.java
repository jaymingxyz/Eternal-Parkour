package io.github.jaymingxyz.eternalparkour.core.menu.settings;

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

/**
 * The menu for settings
 */
public class SettingsMenu extends DynamicMenu {

    public SettingsMenu() {
        registerMainItem(1, 0, (player, user) -> Locales.getItem(player, "settings.parkour_settings.item").click(event -> {
            ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());

            if (pp != null) {
                pp.session.generator.menu(pp);
            }
        }), player -> ParkourOption.PARKOUR_SETTINGS.mayPerform(player) && ParkourPlayer.isPlayer(player));

        registerMainItem(1, 1, (player, user) -> Locales.getItem(player, "settings.lang.item", user != null ? Locales.getString(user.locale, "name") : "?").click(event -> Menus.LANG.open(ParkourPlayer.getPlayer(event.getPlayer()))), player -> ParkourOption.LANG.mayPerform(player) && ParkourUser.isUser(player));

        registerMainItem(1, 2, (player, user) -> {
            // user has to be not-null to see this item
            assert user != null;

            List<String> values = Locales.getStringList(user.locale, "settings.chat.values");

            return new SliderItem().initial(switch (user.chatType) {
                case LOBBY_ONLY -> 0;
                case PLAYERS_ONLY -> 1;
                case PUBLIC -> 2;
            }).add(0, Locales.getItem(player, "settings.chat").modifyLore(lore -> lore.replace("%s", values.get(0))), event -> { // lobby only
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.chatType = Session.ChatType.LOBBY_ONLY;
                }

                return true;
            }).add(1, Locales.getItem(player, "settings.chat").modifyLore(lore -> lore.replace("%s", values.get(1))), event -> { // players only
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.chatType = Session.ChatType.PLAYERS_ONLY;
                }

                return true;
            }).add(2, Locales.getItem(player, "settings.chat").modifyLore(lore -> lore.replace("%s", values.get(2))), event -> { // public
                ParkourUser u = ParkourUser.getUser(event.getPlayer());

                if (u != null) {
                    u.chatType = Session.ChatType.PUBLIC;
                }

                return true;
            });
        }, player -> ParkourOption.CHAT.mayPerform(player) && ParkourUser.isUser(player));

        registerMainItem(2, 0, (player, user) -> MenuStyle.close(MenuStyle.locale(player)), player -> true);
    }

    public void open(Player player) {
        display(player, MenuStyle.frame(new Menu(3, Locales.getString(player, "settings.name"))).distributeRowsEvenly());
    }
}