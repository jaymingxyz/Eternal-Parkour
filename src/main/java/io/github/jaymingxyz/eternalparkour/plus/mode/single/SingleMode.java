package io.github.jaymingxyz.eternalparkour.plus.mode.single;

import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public abstract class SingleMode implements Mode {

    @Override
    public @NotNull Item getItem(String locale) {
        return PlusLocales.getItem(locale, "play.single.%s".formatted(getName()));
    }

    /**
     * Avoids repetition in creating single modes.
     *
     * @param player    The player.
     * @param generator The generator function.
     */
    protected void create(Player player, Function<Session, ParkourGenerator> generator) {
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);
        if (pp != null && pp.session.generator.getMode().getName().equals(getName())) {
            return;
        }
        player.closeInventory();

        Session.create(generator, null, null, player);
    }
}