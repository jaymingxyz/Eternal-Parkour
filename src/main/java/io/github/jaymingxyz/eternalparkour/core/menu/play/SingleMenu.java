package io.github.jaymingxyz.eternalparkour.core.menu.play;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The menu to select single player modes
 */
public class SingleMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        List<Mode> modes = Registry.getModes();

        List<MenuItem> items = new ArrayList<>();
        List<Mode> modeSet = new ArrayList<>();
        for (Mode mode : modes) {
            boolean permissions = Config.CONFIG.getBoolean("permissions.enabled") && !player.hasPermission("ip.gamemode." + mode.getName());

            Item item = mode.getItem(locale);

            if (permissions || mode instanceof MultiMode || item == null) {
                continue;
            }

            modeSet.add(mode);

            items.add(MenuStyle.hint(item.clone(), MenuStyle.hint(locale, "play")).click(event -> {
                if (user == null || Duration.between(user.joined, Instant.now()).toSeconds() > 3) {
                    mode.create(player);
                }
            }));
        }

        if (modeSet.size() == 1) {
            modeSet.get(0).create(player);
            return;
        }

        PagedMenu mode = MenuStyle.list(Locales.getString(player, "play.single.name"), locale);
        mode.addToDisplay(items)
                .item(MenuStyle.center(mode), MenuStyle.back(locale, event -> Menus.PLAY.open(event.getPlayer())))
                .open(player);
    }

}
