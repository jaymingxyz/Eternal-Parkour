package io.github.jaymingxyz.eternalparkour.core.mode;

import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourSpectator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpectatorMode implements Mode {

    @Override
    public @NotNull String getName() {
        return "spectator";
    }

    @Override
    @Nullable
    public Item getItem(String locale) {
        return null;
    }

    @Override
    @Nullable
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public void create(Player player) {
        Menus.SPECTATOR.open(player);
    }

    public void create(Player player, Session session) {
        if (!Config.CONFIG.getBoolean("joining")) {
            player.sendMessage(Strings.colour("<red><bold>Joining is currently disabled."));
            return;
        }
        if (!ParkourOption.SPECTATOR.mayPerform(player)) {
            player.sendMessage(Locales.getString(player, "other.no_do"));
            return;
        }

        ParkourUser user = ParkourUser.getUser(player);
        ParkourSpectator spectator;

        if (session.getPlayers().isEmpty()) {
            return;
        }

        if (user != null) {
            ParkourUser.unregister(user, false, false, false);
            spectator = new ParkourSpectator(player, session, user.previousData);
        } else {
            spectator = new ParkourSpectator(player, session, null);
        }

        session.addSpectators(spectator);
    }
}
