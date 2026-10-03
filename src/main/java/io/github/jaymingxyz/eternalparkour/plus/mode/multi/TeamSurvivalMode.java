package io.github.jaymingxyz.eternalparkour.plus.mode.multi;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.menu.community.SingleLeaderboardMenu;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusConfigOption;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.generator.multi.TeamSurvivalGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class TeamSurvivalMode implements MultiMode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    public void create(Player player) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator instanceof TeamSurvivalGenerator) {
            return;
        }
        player.closeInventory();

        Session.create(TeamSurvivalGenerator::new,
                session -> session.getPlayers().size() < PlusConfigOption.TEAM_SURVIVAL_MAX_COUNT,
                null,
                player);
    }

    @Override
    public void join(Player player, Session session) {
        if (!session.isAcceptingPlayers()) {
            return;
        }
        player.closeInventory();

        ParkourPlayer pp = ParkourUser.register(player, session);
        session.addPlayers(pp);
        pp.setup(session.generator.playerSpawn);
    }

    @Override
    public void leave(Player player, Session session) {

    }

    @Override
    public int getMaxPlayers() {
        return PlusConfigOption.TEAM_SURVIVAL_MAX_COUNT;
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return PlusLocales.getItem(locale, "play.multi.%s".formatted(getName()));
    }

    @Override
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public @NotNull String getName() {
        return "team_survival";
    }
}
