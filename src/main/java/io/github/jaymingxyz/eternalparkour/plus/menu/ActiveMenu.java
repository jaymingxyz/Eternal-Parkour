package io.github.jaymingxyz.eternalparkour.plus.menu;

import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourSpectator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.world.Divider;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Opens the Session menu
 */
public class ActiveMenu {

    public static void open(Player player, MenuSort sort) {
        String locale = MenuStyle.locale(player);
        PagedMenu menu = MenuStyle.list(PlusLocales.getString(player, "active.name", false), locale);
        int bar = MenuStyle.center(menu);

        ParkourUser user = ParkourUser.getUser(player);

        List<Session> sessions = new ArrayList<>(); // get all public sessions
        for (Session session : Divider.sections.keySet()) {
            if (user != null && user.session == session) {
                continue;
            }

            if (session.getVisibility() != Session.Visibility.PUBLIC) { // only display public sessions
                continue;
            }

            if (session.getPlayers().isEmpty()) {
                continue;
            }

            sessions.add(session);
        }

        // sort all sessions by available player count
        sessions = getSessions(sort, sessions); // sort sessions

        List<MenuItem> items = new ArrayList<>();
        for (Session session : sessions) { // turn sessions into items
            items.add(getItem(player, locale, session));
        }

        menu.addToDisplay(items)
                .item(bar - 2, PlusLocales.getItem(player, "active.refresh").click(event -> open(player, sort)))
                .item(bar, MenuStyle.back(locale, event -> Menus.COMMUNITY.open(event.getPlayer())))
                .item(bar + 2, PlusLocales.getItem(player, "active.sort").click(event -> {
                    if (sort == MenuSort.LEAST_OPEN_FIRST) {
                        open(player, MenuSort.LEAST_OPEN_LAST);
                    } else {
                        open(player, MenuSort.LEAST_OPEN_FIRST);
                    }
                }))
                .open(player);
    }

    // Green while there's room, orange for the last spot, red when full (then only spectating is possible).
    @NotNull
    private static MenuItem getItem(Player player, String locale, Session session) {
        int max = session.generator.getMode() instanceof MultiMode multiMode ? multiMode.getMaxPlayers() : 1;
        int openSpaces = max - session.getPlayers().size();

        String main = "<#59DB3E>";
        String accent = "<#C8F2C0>";
        Material material = Material.LIME_STAINED_GLASS_PANE;
        if (openSpaces == 1) {
            main = "<#DB973E>";
            accent = "<#F2D9C0>";
            material = Material.ORANGE_STAINED_GLASS_PANE;
        } else if (openSpaces <= 0) {
            main = "<#DB3E3E>";
            accent = "<#F2C0C0>";
            material = Material.RED_STAINED_GLASS_PANE;
        }

        Mode mode = session.generator.getMode();
        Item modeItem = mode.getItem(locale);
        String modeName = modeItem == null ? mode.getName() : Strings.stripTags(modeItem.getName());

        List<String> lore = new ArrayList<>();
        lore.add(text(player, "active.lobby.players").formatted(accent + session.getPlayers().size() + "<dark_gray>/" + max));
        lore.add(text(player, "active.lobby.mode").formatted(accent + modeName));
        lore.add("");
        lore.add(text(player, "active.lobby.playing"));
        for (ParkourPlayer pp : session.getPlayers()) {
            lore.add("<dark_gray>• <gray>%s".formatted(pp.getName()));
        }
        if (!session.getSpectators().isEmpty()) {
            lore.add(text(player, "active.lobby.spectating"));
            for (ParkourSpectator spectator : session.getSpectators()) {
                lore.add("<dark_gray>• <gray>%s".formatted(spectator.getName()));
            }
        }

        Item item = new Item(material, main + "<bold>" + text(player, "active.lobby.name").formatted(session.getPlayers().getFirst().getName()))
                .lore(lore);

        if (openSpaces > 0) {
            return MenuStyle.hint(item, MenuStyle.hint(locale, "join")).click(event -> {
                if (!Divider.sections.containsKey(session) || !(session.generator.getMode() instanceof MultiMode multiMode)) {
                    return;
                }

                multiMode.join(player, session);
            });
        }

        if (!session.isAcceptingSpectators()) {
            return item;
        }

        return MenuStyle.hint(item, accent + text(player, "active.lobby.full")).click(event -> {
            ParkourUser other = ParkourUser.getUser(event.getPlayer());
            if (!Divider.sections.containsKey(session) || (other != null && session == other.session)) {
                return;
            }

            Modes.SPECTATOR.create(player, session);
        });
    }

    private static String text(Player player, String path) {
        return PlusLocales.getString(player, path, false);
    }

    @NotNull
    private static List<Session> getSessions(MenuSort sort, List<Session> sessions) {
        List<Session> list = new ArrayList<>(sessions);

        list.sort((session1, session2) -> {
            int max1 = 1;
            if (session1.generator.getMode() instanceof MultiMode multiMode) {
                max1 = multiMode.getMaxPlayers();
            }
            int max2 = 1;
            if (session2.generator.getMode() instanceof MultiMode multiMode) {
                max2 = multiMode.getMaxPlayers();
            }

            int open1 = max1 - session1.getPlayers().size();
            int open2 = max2 - session2.getPlayers().size();

            if (sort == MenuSort.LEAST_OPEN_FIRST) {
                return open1 - open2;
            } else {
                return open2 - open1;
            }
        });

        return list;
    }

    public enum MenuSort {
        LEAST_OPEN_FIRST,
        LEAST_OPEN_LAST
    }
}
