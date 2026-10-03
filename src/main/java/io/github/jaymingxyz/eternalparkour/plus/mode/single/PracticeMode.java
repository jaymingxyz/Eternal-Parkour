package io.github.jaymingxyz.eternalparkour.plus.mode.single;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.PracticeGenerator;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class PracticeMode extends SingleMode {

    @Override
    public void create(Player player) {
        create(player, PracticeGenerator::new);
    }

    @Override
    public @NotNull Item getItem(String locale) {
        return PlusLocales.getItem(locale, "play.single.%s.item".formatted(getName()));
    }

    @Override
    public Leaderboard getLeaderboard() {
        return null;
    }

    @Override
    public @NotNull String getName() {
        return "practice";
    }
}
