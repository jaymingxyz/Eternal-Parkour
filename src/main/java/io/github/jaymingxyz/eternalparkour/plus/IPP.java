package io.github.jaymingxyz.eternalparkour.plus;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Logging;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusConfig;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.generator.single.PracticeGenerator;
import io.github.jaymingxyz.eternalparkour.plus.menu.ActiveMenu;
import io.github.jaymingxyz.eternalparkour.plus.menu.InviteMenu;
import io.github.jaymingxyz.eternalparkour.plus.menu.MultiplayerMenu;
import io.github.jaymingxyz.eternalparkour.plus.mode.PlusMode;
import io.github.jaymingxyz.eternalparkour.plus.mode.lobby.Lobby;
import io.github.jaymingxyz.eternalparkour.plus.mode.multi.DuelsMode;
import io.github.jaymingxyz.eternalparkour.plus.mode.multi.TeamSurvivalMode;
import io.github.jaymingxyz.eternalparkour.plus.mode.single.*;
import io.github.jaymingxyz.eternalparkour.plus.style.IncrementalStyle;

import java.io.File;
public final class IPP {

    public static final String PREFIX = EternalParkour.PREFIX;
    private static EternalParkour plugin;
    private static Logging logging;
    private static PlusConfig configuration;

    private IPP() {
    }

    /**
     * Returns the {@link Logging} belonging to this plugin.
     *
     * @return this plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return logging;
    }

    /**
     * Returns this plugin instance.
     *
     * @return the plugin instance.
     */
    public static EternalParkour getPlugin() {
        return plugin;
    }

    public static File getDataFolder() {
        return new File(plugin.getDataFolder(), "plus");
    }

    public static PlusConfig getConfiguration() {
        return configuration;
    }

    public static void enable(EternalParkour owner) {
        plugin = owner;
        logging = new Logging(owner);
        configuration = new PlusConfig(owner);
        configuration.reload();

        // Events
        owner.registerListener(new PlusHandler());
        owner.registerCommand("ipp", new PlusCommand());

        // Gamemode register
        registerMode(new PracticeMode());
        registerMode(new TeamSurvivalMode());
        registerMode(new LobbyMode());
        registerMode(new SpeedMode());
        registerMode(new SuperJumpMode());
        registerMode(new HourglassMode());
        registerMode(new TimeTrialMode());
        registerMode(new DuelsMode());

        registerMode(new WaveTrialMode());

        Lobby.read();
        PlusMode.init();

        // Register stuff for main menu
        // Multiplayer if player is not found
        Menus.PLAY.registerMainItem(1, 5,
                (player, user) -> PlusLocales.getItem(player, "play.multi.item")
                        .click(event -> MultiplayerMenu.open(event.getPlayer())),
                PlusOption.MULTIPLAYER::mayPerform);

        // practice settings only if player's generator is of this instance
        Menus.SETTINGS.registerMainItem(1, 3,
                (player, user) -> PlusLocales.getItem(player, "play.single.practice.items.settings").click(
                        event -> {
                            ParkourPlayer pp = ParkourPlayer.getPlayer(event.getPlayer());
                            if (pp != null && pp.session.generator instanceof PracticeGenerator generator) {
                                generator.open();
                            }
                        }),
                player -> {
                    ParkourPlayer pp = ParkourPlayer.getPlayer(player);
                    return PlusOption.PRACTICE_SETTINGS.mayPerform(player) &&
                            pp != null &&
                            pp.session.generator instanceof PracticeGenerator;
                });

        Menus.LOBBY.registerMainItem(1, 2,
                (player, user) -> PlusLocales.getItem(player, "invite.item")
                        .click(event -> InviteMenu.open(event.getPlayer())),
                player -> {
                    ParkourUser user = ParkourUser.getUser(player);

                    // only show is user is parkourplayer and first player in session (the owner)
                    return PlusOption.INVITE.mayPerform(player) &&
                            user instanceof ParkourPlayer &&
                            user.session.generator.getMode() instanceof MultiMode &&
                            user.session.getPlayers().get(0) == user;
                });

        Menus.COMMUNITY.registerMainItem(1, 1,
                (player, user) -> PlusLocales.getItem(player, "active.item")
                        .click(event -> ActiveMenu.open(event.getPlayer(), ActiveMenu.MenuSort.LEAST_OPEN_FIRST)),
                player -> PlusOption.ACTIVE.mayPerform(player) && Config.CONFIG.getBoolean("joining"));

        if (configuration.getFile("config").getBoolean("styles.incremental.enabled")) {
            Option.initStyles("styles.incremental.list", configuration.getFile("config"), IncrementalStyle::new)
                    .forEach(Registry::register);
        }

    }

    public static void disable() {
        // save all gamemodes
        if (PlusMode.TIME_TRIAL != null) {
            PlusMode.TIME_TRIAL.getLeaderboard().write(false);
        }
        if (PlusMode.SUPER_JUMP != null) {
            PlusMode.SUPER_JUMP.getLeaderboard().write(false);
        }
        if (PlusMode.HOURGLASS != null) {
            PlusMode.HOURGLASS.getLeaderboard().write(false);
        }
        if (PlusMode.SPEED != null) {
            PlusMode.SPEED.getLeaderboard().write(false);
        }
        if (PlusMode.WAVE_TRIAL != null) {
            PlusMode.WAVE_TRIAL.getLeaderboard().write(false);
        }
        if (PlusMode.TEAM_SURVIVAL != null) {
            PlusMode.TEAM_SURVIVAL.getLeaderboard().write(false);
        }
    }

    private static void registerMode(Mode gamemode) {
        if (configuration.getFile("config").getBoolean("gamemodes.%s.enabled".formatted(gamemode.getName().toLowerCase()))) {
            Registry.register(gamemode);
        }
    }
}
