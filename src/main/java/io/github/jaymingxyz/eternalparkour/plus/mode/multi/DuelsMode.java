package io.github.jaymingxyz.eternalparkour.plus.mode.multi;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusConfigOption;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.generator.multi.DuelsGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class DuelsMode implements MultiMode {

    @Override
    public void create(Player player) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof DuelsGenerator) {
            return;
        }
        player.closeInventory();

        Session.create(DuelsGenerator::new,
                        session -> session.getPlayers().size() < PlusConfigOption.DUELS_MAX_COUNT && ((DuelsGenerator) session.generator).allowJoining,
                        null,
                        player);
    }

    @Override
    public void join(Player player, Session session) {
        if (!session.isAcceptingPlayers()) {
            return;
        }

        player.closeInventory();

        DuelsGenerator generator = (DuelsGenerator) session.generator;

        ParkourPlayer pp = ParkourUser.register(player, session);
        session.addPlayers(pp);
        generator.addPlayer(pp);
        pp.setup(null);
    }

    @Override
    public void leave(Player player, Session session) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (pp == null || session.getPlayers().isEmpty()) {
            return;
        }

        ((DuelsGenerator) session.generator).removePlayer(pp);
    }

    @Override
    public int getMaxPlayers() {
        return PlusConfigOption.DUELS_MAX_COUNT;
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return PlusLocales.getItem(locale, "play.multi.%s.item".formatted(getName()));
    }

    @Override
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return "duels";
    }
}