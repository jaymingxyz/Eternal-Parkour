package io.github.jaymingxyz.eternalparkour.plus.generator.single;

import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.generator.GeneratorOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.mode.PlusMode;
import org.bukkit.Material;

/**
 * Generator for Lobby mode
 */
public final class LobbyGenerator extends PlusGenerator {

    public LobbyGenerator(Session session) {
        // setup generator settings
        super(session, (Schematic) null, GeneratorOption.DISABLE_SCHEMATICS);

        // setup menu
        menu = new ParkourSettingsMenu(ParkourOption.SCHEMATICS, ParkourOption.LEADS);

        // remove -2 jumps
        heightChances.clear();
        heightChances.put(1, Option.NORMAL_HEIGHT_1);
        heightChances.put(0, Option.NORMAL_HEIGHT_0);
        heightChances.put(-1, Option.NORMAL_HEIGHT_NEG1);

        // remove 1 block jumps
        distanceChances.clear();
        distanceChances.put(2, Option.NORMAL_DISTANCE_2);
        distanceChances.put(3, Option.NORMAL_DISTANCE_3);
        distanceChances.put(4, Option.NORMAL_DISTANCE_4);
    }

    @Override
    public void overrideProfile() {
        profile.set("blockLead", "4");
        profile.set("useSchematic", "false");
    }

    @Override
    public void generate() {
        super.generate();

        island.blocks.get(0).setType(Material.SMOOTH_QUARTZ);
    }

    @Override
    public Mode getMode() {
        return PlusMode.LOBBY;
    }
}