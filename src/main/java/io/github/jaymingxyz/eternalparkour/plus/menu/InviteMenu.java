package io.github.jaymingxyz.eternalparkour.plus.menu;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Cooldowns;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.SkullSetter;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class InviteMenu {

    public static void open(Player player) {
        List<MenuItem> items = new ArrayList<>();
        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        PagedMenu playerMenu = MenuStyle.list(PlusLocales.getString(player, "invite.name", false), user.locale);
        int bar = MenuStyle.center(playerMenu);
        Session session = user.session;

        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            Item item = PlusLocales.getItem(user.locale, "invite.head", other.getName())
                    .material(Material.PLAYER_HEAD);

            ItemStack stack = item.build();
            stack = stack.withType(Material.PLAYER_HEAD);

            // bedrock has no player skull support
            if (!ParkourUser.isBedrockPlayer(player)) {
                SkullMeta meta = (SkullMeta) stack.getItemMeta();

                if (meta != null) {
                    SkullSetter.setPlayerHead(other, meta);
                    item.meta(meta);
                }
            }

            items.add(MenuStyle.hint(item, MenuStyle.hint(user.locale, "invite")).click(event -> {
                if (Cooldowns.canPerform(player, "multiplayer invite", 2500)) {
                    for (String s : PlusLocales.getString(other, "invite.message", false).formatted(player.getName(),
                            Strings.stripTags(session.generator.getMode().getItem(Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG)).getName()),
                            player.getName()).split("\\|\\|")) {
                        if (classExists()) {
                            new AdventureInviteSender(other, player, s);
                        } else {
                            send(other, s);
                        }
                    }

                    send(player, PlusLocales.getString(player, "invite.success", false).formatted(other.getName()));
                }
            }));
        }

        playerMenu
                .addToDisplay(items)
                .item(bar - 2, PlusLocales.getItem(player, "invite.lobby", player.getName(), player.getName()))
                .item(bar, MenuStyle.back(user.locale, event -> Menus.LOBBY.open(event.getPlayer())))
                .open(player);
    }

    private static boolean classExists() {
        try {
            Class.forName("net.kyori.adventure.text.minimessage.MiniMessage");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}
