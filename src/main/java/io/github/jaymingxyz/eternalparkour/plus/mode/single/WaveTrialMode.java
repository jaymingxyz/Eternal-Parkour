package io.github.jaymingxyz.eternalparkour.plus.mode.single;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.menu.community.SingleLeaderboardMenu;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.WaveTrialGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class WaveTrialMode extends SingleMode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.TIME);

    @Override
    public void create(Player player) {
        create(player, WaveTrialGenerator::new);
    }

    @Override
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public @NotNull String getName() {
        return "wave_trial";
    }
}