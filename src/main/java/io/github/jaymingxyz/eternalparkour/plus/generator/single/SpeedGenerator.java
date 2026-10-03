package io.github.jaymingxyz.eternalparkour.plus.generator.single;

import io.github.jaymingxyz.eternalparkour.core.generator.GeneratorOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.IPP;
import io.github.jaymingxyz.eternalparkour.plus.mode.PlusMode;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Class for multiplayer
 */
public final class SpeedGenerator extends PlusGenerator {

    public SpeedGenerator(Session session) {
        // setup generator settings
        super(session, GeneratorOption.DISABLE_SCHEMATICS, GeneratorOption.DISABLE_SPECIAL, GeneratorOption.REDUCE_RANDOM_BLOCK_SELECTION_ANGLE);

        // setup menu
        menu = new ParkourSettingsMenu(ParkourOption.SCHEMATICS, ParkourOption.SPECIAL_BLOCKS);

        distanceChances.clear();
        distanceChances.put(1, 1.0);

        heightChances.clear();
        heightChances.put(0, 1.0);

        // Only while still playing this mode: added after leaving, the effect would last ~80 minutes.
        Task.create(IPP.getPlugin())
                .delay(10)
                .execute(() -> {
                    if (!stopped && player.isActive() && player.session == session) {
                        player.player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100000, 2, false, false));
                    }
                })
                .run();
    }

    @Override
    public void overrideProfile() {
        profile.set("blockLead", "10");
    }

    @Override
    public Mode getMode() {
        return PlusMode.SPEED;
    }
}