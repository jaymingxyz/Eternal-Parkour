package io.github.jaymingxyz.eternalparkour.plus.generator.single;

import io.github.jaymingxyz.eternalparkour.core.generator.GeneratorOption;
import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * DefaultGenerator wrap for IP+.
 */
public abstract class PlusGenerator extends ParkourGenerator {

    protected ParkourSettingsMenu menu;

    public PlusGenerator(@NotNull Session session, GeneratorOption... generatorOptions) {
        super(session, generatorOptions);
    }

    public PlusGenerator(@NotNull Session session, @Nullable Schematic schematic, GeneratorOption... generatorOptions) {
        super(session, schematic, generatorOptions);
    }

    @Override
    public void menu(ParkourPlayer player) {
        menu.open(player);
    }
}
