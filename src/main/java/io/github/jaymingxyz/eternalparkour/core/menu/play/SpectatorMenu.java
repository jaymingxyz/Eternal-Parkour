package io.github.jaymingxyz.eternalparkour.core.menu.play;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.world.Divider;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.SkullSetter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * The menu to select other players to spectate
 */
public class SpectatorMenu {

    public void open(Player player) {
        ParkourUser user = ParkourUser.getUser(player);
        String locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;

        PagedMenu spectator = MenuStyle.list(Locales.getString(player, "play.spectator.name"), locale);

        List<MenuItem> display = new ArrayList<>();

        for (Session session : Divider.sections.keySet()) {
            if (!session.isAcceptingSpectators()) { // only showcase sessions with spectators enabled
                continue;
            }

            if (user != null && session == user.session) { // don't let player join their own session
                continue;
            }

            if (session.getPlayers().isEmpty()) { // weird but possible
                continue;
            }

            var pp = session.getPlayers().get(0);

            Item item = Locales.getItem(locale, "play.spectator.head", pp.getName());
            // Player head gathering
            item.material(Material.PLAYER_HEAD);

            ItemStack stack = item.build(); // Updating meta requires building
            stack = stack.withType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (!ParkourUser.isBedrockPlayer(player)) {
                if (pp.getName() != null && !pp.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(pp.player, meta);
                        item.meta(meta);
                    }
                }
            }

            MenuStyle.hint(item, MenuStyle.hint(locale, "spectate")).click(event -> Modes.SPECTATOR.create(player, session));

            display.add(item);
        }

        spectator
                .addToDisplay(display)
                .item(MenuStyle.center(spectator), MenuStyle.back(locale, event -> Menus.PLAY.open(event.getPlayer())))
                .open(player);

    }

}
