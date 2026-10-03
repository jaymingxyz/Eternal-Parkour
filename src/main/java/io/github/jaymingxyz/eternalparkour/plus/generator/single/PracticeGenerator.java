package io.github.jaymingxyz.eternalparkour.plus.generator.single;

import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.SliderItem;
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.menu.settings.ParkourSettingsMenu;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.mode.PlusMode;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

/**
 * Class for multiplayer
 */
public final class PracticeGenerator extends PlusGenerator {

    // ensure same instances to ensure working ==
    private static final BlockData PACKED_ICE = Material.PACKED_ICE.createBlockData();
    private static final BlockData SMOOTH_QUARTZ_SLAB = Material.SMOOTH_QUARTZ_SLAB.createBlockData("[type=bottom]");
    private static final BlockData GLASS_PANE = Material.GLASS_PANE.createBlockData();
    private static final BlockData OAK_FENCE = Material.OAK_FENCE.createBlockData();

    public PracticeGenerator(Session session) {
        super(session);

        menu = new ParkourSettingsMenu(ParkourOption.SPECIAL_BLOCKS);

        distanceChances.clear();
        distanceChances.put(1, 1.0);
        distanceChances.put(2, 1.0);
        distanceChances.put(3, 1.0);
        distanceChances.put(4, 1.0);

        specialChances.clear();
        specialChances.put(PACKED_ICE, 1.0);
        specialChances.put(SMOOTH_QUARTZ_SLAB, 1.0);
        specialChances.put(GLASS_PANE, 1.0);
        specialChances.put(OAK_FENCE, 1.0);

        defaultChances.clear();
        defaultChances.put(JumpType.DEFAULT, 1.0);
        defaultChances.put(JumpType.SPECIAL, 1.0);
        defaultChances.put(JumpType.SCHEMATIC, 1.0);
    }

    /**
     * Opens the menu
     */
    public void open() {
        Menu menu = MenuStyle.frame(new Menu(4, PlusLocales.getString(player.locale, "play.single.practice.name", false)));
        String locale = player.locale;

        menu
                .item(9, new SliderItem()
                        .initial(distanceChances.containsKey(1) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.one_block")
                                .material(Material.LIME_DYE)
                                .modifyName("<green>%s"::formatted), event -> handleDistanceOn(1))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.one_block")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleDistanceOff(1)))

                .item(10, new SliderItem()
                        .initial(distanceChances.containsKey(2) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.two_block")
                                .material(Material.LIME_DYE)
                                .modifyName("<green>%s"::formatted), event -> handleDistanceOn(2))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.two_block")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleDistanceOff(2)))

                .item(11, new SliderItem()
                        .initial(distanceChances.containsKey(3) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.three_block")
                                .material(Material.LIME_DYE)
                                .modifyName("<green>%s"::formatted), event -> handleDistanceOn(3))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.three_block")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleDistanceOff(3)))

                .item(12, new SliderItem()
                        .initial(distanceChances.containsKey(4) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.four_block")
                                .material(Material.LIME_DYE)
                                .modifyName("<green>%s"::formatted), event -> handleDistanceOn(4))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.four_block")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleDistanceOff(4)))

                .item(18, new SliderItem()
                        .initial(defaultChances.containsKey(JumpType.DEFAULT) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.normal")
                                .material(Material.BARREL)
                                .modifyName("<green>%s"::formatted), event -> handleJumpTypeOn(JumpType.DEFAULT))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.normal")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleJumpTypeOff(JumpType.DEFAULT)))

                .item(19, new SliderItem()
                        .initial(defaultChances.containsKey(JumpType.SCHEMATIC) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.schematics")
                                .material(Material.SCAFFOLDING)
                                .modifyName("<green>%s"::formatted), event -> handleJumpTypeOn(JumpType.SCHEMATIC))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.schematics")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleJumpTypeOff(JumpType.SCHEMATIC)))

                .item(20, new SliderItem()
                        .initial(specialChances.containsKey(PACKED_ICE) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.ice")
                                .material(Material.ICE)
                                .modifyName("<green>%s"::formatted), event -> handleSpecialOn(PACKED_ICE))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.ice")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleSpecialOff(PACKED_ICE)))

                .item(21, new SliderItem()
                        .initial(specialChances.containsKey(SMOOTH_QUARTZ_SLAB) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.slabs")
                                .material(Material.SMOOTH_QUARTZ_SLAB)
                                .modifyName("<green>%s"::formatted), event -> handleSpecialOn(SMOOTH_QUARTZ_SLAB))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.slabs")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleSpecialOff(SMOOTH_QUARTZ_SLAB)))

                .item(22, new SliderItem()
                        .initial(specialChances.containsKey(GLASS_PANE) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.glass_panes")
                                .material(Material.GLASS_PANE)
                                .modifyName("<green>%s"::formatted), event -> handleSpecialOn(GLASS_PANE))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.glass_panes")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleSpecialOff(GLASS_PANE)))

                .item(23, new SliderItem()
                        .initial(specialChances.containsKey(OAK_FENCE) ? 0 : 1)
                        .add(0, PlusLocales.getItem(locale, "play.single.practice.items.fences")
                                .material(Material.OAK_FENCE)
                                .modifyName("<green>%s"::formatted), event -> handleSpecialOn(OAK_FENCE))
                        .add(1, PlusLocales.getItem(locale, "play.single.practice.items.fences")
                                .material(Material.GRAY_DYE)
                                .modifyName("<red>%s"::formatted), event -> handleSpecialOff(OAK_FENCE)))

                .item(27, MenuStyle.back(player.locale, event -> menu(player)))

                .distributeRowEvenly(0, 1, 2, 3)
                .open(player.player);
    }

    @Override
    public void generate() {
        super.generate();

        if (schematicCooldown > 1) {
            schematicCooldown = 1;
        }
    }

    private boolean handleJumpTypeOn(JumpType type) {
        defaultChances.put(type, 1.0);
        return true;
    }

    private boolean handleJumpTypeOff(JumpType type) {
        if (defaultChances.size() == 1) {
            return false;
        }

        defaultChances.remove(type);
        return true;
    }

    private boolean handleDistanceOn(int distance) {
        distanceChances.put(distance, 1.0);
        return true;
    }

    private boolean handleDistanceOff(int distance) {
        if (distanceChances.size() == 1) {
            return false;
        }

        distanceChances.remove(distance);
        return true;
    }

    private boolean handleSpecialOn(BlockData type) {
        defaultChances.put(JumpType.SPECIAL, 1.0);
        specialChances.put(type, 1.0);
        return true;
    }

    private boolean handleSpecialOff(BlockData type) {
        if (defaultChances.size() == 1 && specialChances.size() == 1) {
            return false;
        }

        if (specialChances.size() == 1) {
            defaultChances.remove(JumpType.SPECIAL);
        }
        specialChances.remove(type);
        return true;
    }

    @Override
    public Mode getMode() {
        return PlusMode.PRACTICE;
    }
}