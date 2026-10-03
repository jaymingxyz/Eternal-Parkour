package io.github.jaymingxyz.eternalparkour.core.menu.community;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.PagedMenu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.SkullSetter;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Menu for a single leaderboard
 */
public class SingleLeaderboardMenu {

    public void open(Player player, Mode mode, Leaderboard.Sort sort) {
        Leaderboard leaderboard = mode.getLeaderboard();

        if (leaderboard == null) {
            return;
        }

        // init vars
        var user = ParkourUser.getUser(player);
        var locale = user == null ? Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG) : user.locale;
        Item modeItem = mode.getItem(locale);
        String modeName = modeItem == null ? mode.getName() : Strings.stripTags(modeItem.getName());
        var menu = MenuStyle.list(Locales.getString(locale, "%s.title".formatted(ParkourOption.LEADERBOARDS.path)).replace("%s", modeName), locale);
        int bar = MenuStyle.center(menu);

        var items = new ArrayList<MenuItem>();

        var base = Locales.getItem(player, "%s.head".formatted(ParkourOption.LEADERBOARDS.path));

        for (Map.Entry<UUID, Score> entry : leaderboard.sort(sort).entrySet()) {
            int rank = items.size() + 1;

            var uuid = entry.getKey();
            var score = entry.getValue();

            if (score == null) {
                continue;
            }

            Item item = base.clone().material(Material.PLAYER_HEAD)
                    .modifyName(name -> name.replace("%r", Integer.toString(rank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()))
                    .modifyLore(line -> line.replace("%r", Integer.toString(rank))
                            .replace("%s", Integer.toString(score.score()))
                            .replace("%p", score.name())
                            .replace("%t", score.time())
                            .replace("%d", score.difficulty()));

            // gold, silver and bronze for the top three
            if (rank <= 3) {
                String medal = switch (rank) {
                    case 1 -> "<#FFD700><bold>";
                    case 2 -> "<#C0C0C0><bold>";
                    default -> "<#CD7F32><bold>";
                };
                item.modifyName(name -> medal + Strings.stripTags(name));
            }

            // Player head gathering
            ItemStack stack = item.build();

            // if there are more than 36 players, don't show the heads to avoid server crashing
            // and bedrock has no player skull support
            if (rank <= 20 && !ParkourUser.isBedrockPlayer(player)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);

                if (op.getName() != null && !op.getName().startsWith(".")) { // bedrock players' names with geyser start with a .
                    SkullMeta meta = (SkullMeta) stack.getItemMeta();

                    if (meta != null) {
                        SkullSetter.setPlayerHead(op, meta);
                        item.meta(meta);
                    }
                }
            }

            if (uuid.equals(player.getUniqueId())) {
                menu.item(bar - 2, item.clone());
            }

            items.add(item);
        }

        List<String> values = Locales.getStringList(locale, "%s.sort.values".formatted(ParkourOption.LEADERBOARDS.path));

        if (values.size() != 3) {
            EternalParkour.logging().stack("Error while trying to get locales for sort values: not enough sort values present",
                    "check your %s locale file".formatted(locale), new IllegalArgumentException());
        }

        String name = switch (sort) {
            case SCORE -> values.get(0);
            case TIME -> values.get(1);
            case DIFFICULTY -> values.get(2);
        };

        // get next sorting type
        var next = switch (sort) {
            case SCORE -> Leaderboard.Sort.TIME;
            case TIME -> Leaderboard.Sort.DIFFICULTY;
            default -> Leaderboard.Sort.SCORE;
        };

        menu.addToDisplay(items)
                .item(bar + 2, MenuStyle.hint(Locales.getItem(player, ParkourOption.LEADERBOARDS.path + ".sort", name.toLowerCase()), MenuStyle.hint(locale, "change")).click(event -> open(player, mode, next)))
                .item(bar, MenuStyle.back(locale, event -> {
                    List<Mode> modes = Registry.getModes()
                            .stream()
                            .filter(m -> m.getLeaderboard() != null && m.getItem(locale) != null)
                            .toList();

                    if (modes.size() == 1) {
                        Menus.COMMUNITY.open(player);
                        return;
                    }

                    Menus.LEADERBOARDS.open(event.getPlayer());
                }))
                .open(player);
    }
}