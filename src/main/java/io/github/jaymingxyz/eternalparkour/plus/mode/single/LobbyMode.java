package io.github.jaymingxyz.eternalparkour.plus.mode.single;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.LobbyGenerator;
import io.github.jaymingxyz.eternalparkour.plus.mode.lobby.Lobby;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LobbyMode implements Mode {

    @Override
    public void create(Player player) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof LobbyGenerator) {
            return;
        }

        if (Lobby.getSelections().isEmpty() || !Lobby.getSelections().containsKey(player.getWorld())) {
            return;
        }

        player.closeInventory();

        Session session = Session.create(LobbyGenerator::new, null, null, player);

        Lobby.join(session);
    }

    @Override
    public Item getItem(String locale) {
        return null;
    }

    @Override
    public Leaderboard getLeaderboard() {
        return Modes.DEFAULT.getLeaderboard();
    }

    @Override
    public @NotNull String getName() {
        return "lobby";
    }
}