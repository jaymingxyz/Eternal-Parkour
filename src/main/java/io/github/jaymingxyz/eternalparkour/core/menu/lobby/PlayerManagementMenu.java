package io.github.jaymingxyz.eternalparkour.core.menu.lobby;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.SkullSetter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Menu for managing players
 */
public class PlayerManagementMenu {

    public void open(Player p) {
        if (p == null) {
            return;
        }

        ParkourPlayer viewer = ParkourPlayer.getPlayer(p);

        if (viewer == null) {
            return;
        }

        Session session = viewer.session;

        PagedMenu menu = MenuStyle.list(Locales.getString(viewer.locale, "lobby.player_management.name"), viewer.locale);
        add(menu, viewer, session.getPlayers().stream().map(player -> (ParkourUser) player).toList());
        add(menu, viewer, session.getSpectators().stream().map(player -> (ParkourUser) player).toList());

        menu
                .item(MenuStyle.center(menu), MenuStyle.back(viewer.locale, event -> Menus.LOBBY.open(event.getPlayer())))
                .open(p);
    }

    private void add(PagedMenu menu, ParkourUser viewer, Collection<ParkourUser> users) {
        Session session = viewer.session;

        for (ParkourUser other : users) {
            if (other == viewer) {
                continue;
            }

            Item item = Locales.getItem(viewer.locale, "lobby.player_management.head", other.getName());
            item.material(Material.PLAYER_HEAD);

            boolean muted = session.isMuted(other);

            List<String> lore = new ArrayList<>();
            if (muted) {
                // add top
                lore.addAll(List.of(Locales.getString(viewer.locale, "lobby.player_management.head.top").split("\\|\\|")));
            }

            // add bottom
            lore.addAll(List.of(Locales.getString(viewer.locale, "lobby.player_management.head.bottom").split("\\|\\|")));

            // Player head gathering
            item.material(Material.PLAYER_HEAD).lore(lore).click(event -> {
                ClickType click = event.event().getClick();

                switch (click) {
                    case LEFT -> {
                        Modes.DEFAULT.create(other.player);

                        other.sendTranslated("lobby.player_management.kicked");

                        viewer.sendTranslated("lobby.player_management.advice");
                        open(viewer.player);
                    }
                    case RIGHT -> {
                        session.toggleMute(other);

                        if (!muted) {
                            other.sendTranslated("lobby.player_management.muted");
                        } else {
                            other.sendTranslated("lobby.player_management.unmuted");
                        }

                        open(viewer.player);
                    }
                }
            });

            ItemStack stack = item.build(); // Updating meta requires building
            stack = stack.withType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (menu.getTotalToDisplay().size() <= 36 && !ParkourUser.isBedrockPlayer(other.player)) {
                if (other.getName() != null && !other.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(other.player, meta);
                        item.meta(meta);
                    }
                }
            }

            menu.addToDisplay(List.of(item));
        }
    }
}