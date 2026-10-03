package io.github.jaymingxyz.eternalparkour.plus.generator.multi;

import io.github.jaymingxyz.eternalparkour.core.generator.GeneratorOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.PlusGenerator;

public abstract class MultiplayerGenerator extends PlusGenerator {

    protected ParkourSettingsMenu menu;

    public MultiplayerGenerator(Session session, GeneratorOption... options) {
        super(session, options);
    }

    public MultiplayerGenerator(Session session, Schematic schematic, GeneratorOption... options) {
        super(session, schematic, options);
    }

    @Override
    public void menu(ParkourPlayer player) {
        menu.open(player);
    }
}