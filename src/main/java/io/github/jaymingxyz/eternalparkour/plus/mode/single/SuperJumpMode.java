package io.github.jaymingxyz.eternalparkour.plus.mode.single;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.menu.community.SingleLeaderboardMenu;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.SuperJumpGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class SuperJumpMode extends SingleMode {

    private final Leaderboard leaderboard = new Leaderboard(getName(), Leaderboard.Sort.SCORE);

    @Override
    public void create(Player player) {
        create(player, SuperJumpGenerator::new);
    }

    @Override
    public Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public @NotNull String getName() {
        return "super_jump";
    }
}
