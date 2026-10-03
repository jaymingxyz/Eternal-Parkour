package io.github.jaymingxyz.eternalparkour.core;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.api.ServerIntegration;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.hologram.HologramManager;
import io.github.jaymingxyz.eternalparkour.core.hook.PAPIHook;
import io.github.jaymingxyz.eternalparkour.core.integration.IntegrationLoader;
import io.github.jaymingxyz.eternalparkour.core.mode.DefaultMode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.mode.SpectatorMode;
import io.github.jaymingxyz.eternalparkour.core.migration.LegacyDataMigrator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.player.data.ParkourItems;
import io.github.jaymingxyz.eternalparkour.core.storage.Storage;
import io.github.jaymingxyz.eternalparkour.core.world.Divider;
import io.github.jaymingxyz.eternalparkour.core.world.World;
import io.github.jaymingxyz.eternalparkour.elytra.IEP;
import io.github.jaymingxyz.eternalparkour.plus.IPP;
import io.github.jaymingxyz.eternalparkour.core.foundation.ParkourPlugin;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Logging;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.VoidGenerator;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public final class EternalParkour extends ParkourPlugin {

    public static final String NAME = "<#FF6464><bold>Eternal Parkour<reset>";
    public static final String PREFIX = NAME + " <dark_gray>» <gray>";

    /**
     * First line of every help message.
     */
    public static final String HEADER = "<dark_gray><strikethrough>---------------<reset> %s <dark_gray><strikethrough>---------------<reset>".formatted(NAME);

    private static Logging logging;
    private static EternalParkour instance;
    private boolean plusEnabled;
    private static boolean elytraEnabled;
    private List<ServerIntegration> serverIntegrations = List.of();

    @Nullable
    private static PAPIHook placeholderHook;
    @Nullable
    private static PAPIHook legacyPlaceholderHook;

    public static void log(String message) {
        if (Config.CONFIG.getBoolean("debug")) {
            logging.info("[Debug] " + message);
        }
    }

    /**
     * @param child The file name.
     * @return A file from within the plugin folder.
     */
    public static File getInFolder(String child) {
        return new File(instance.getDataFolder(), child);
    }

    /**
     * @return This plugin's {@link Logging} instance.
     */
    public static Logging logging() {
        return logging;
    }

    /**
     * @return The plugin instance.
     */
    public static EternalParkour getPlugin() {
        return instance;
    }

    @Nullable
    public static PAPIHook getPlaceholderHook() {
        return placeholderHook;
    }

    /**
     * @return True when the elytra parkour module is enabled and started successfully.
     */
    public static boolean isElytraEnabled() {
        return elytraEnabled;
    }

    /**
     * Reloads the config and locale files of block parkour, the multiplayer modes and elytra parkour.
     * A module that fails is logged and doesn't stop the others.
     *
     * @return True when every enabled module reloaded.
     */
    public static boolean reloadConfigs() {
        boolean reloaded = runSafely("reloading block parkour", () -> Config.reload(false));

        if (instance.plusEnabled) {
            reloaded &= runSafely("reloading the multiplayer modes", () -> IPP.getConfiguration().reload());
        }
        if (elytraEnabled) {
            reloaded &= runSafely("reloading elytra parkour", IEP.INSTANCE::reload);
        }

        return reloaded;
    }

    @Override
    public void onLoad() {
        instance = this;
        logging = new Logging(this);
    }

    @Override
    public void enable() {

        // ----- Configurations -----

        DataIO.init(this);
        ParkourItems.init(this);
        LegacyDataMigrator.migrate(this);
        Config.reload(true);

        // ----- Registry -----

        Registry.register(new DefaultMode());
        Registry.register(new SpectatorMode());

        Modes.init();
        Menu.init(this);

        // hook with papi after gamemode leaderboards have initialized
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            logging.info("Registered PlaceholderAPI hook");
            placeholderHook = new PAPIHook(PAPIHook.IDENTIFIER);
            placeholderHook.register();
            legacyPlaceholderHook = new PAPIHook(PAPIHook.LEGACY_IDENTIFIER);
            legacyPlaceholderHook.register();
        }

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
            logging.info("Registered BungeeCord hook");
        }

        // ----- Worlds -----

        if (Config.CONFIG.getBoolean("joining")) {
            World.create();
        }

        // ----- Events -----

        registerListener(new Events());
        registerCommand("eternalparkour", new Command());

        try {
            IPP.enable(this);
            plusEnabled = true;
        } catch (Throwable t) {
            logging.stack("Failed to enable the former IPPlus modes", t);
        }

        if (Config.CONFIG.getBoolean("modules.elytra")) {
            try {
                IEP.INSTANCE.enable(this);
                elytraEnabled = true;
            } catch (Throwable t) {
                logging.stack("Failed to enable elytra parkour", t);
            }
        } else {
            logging.info("Elytra parkour is disabled in config.yml (modules.elytra)");

            var iep = getCommand("iep");
            if (iep != null) {
                iep.setExecutor((sender, command, label, args) -> {
                    sender.sendMessage(Strings.colour(PREFIX + "Elytra parkour is disabled on this server."));
                    return true;
                });
            }
        }

        serverIntegrations = IntegrationLoader.enable(this);

        // Built-in hologram leaderboards — replaces DH/HD softdepend on Paper 26.
        // Uses TextDisplay + Interaction (Display API) so there's no armor-stand
        // entity holding chunks open. Initialized after Events so PAPI placeholder
        // resolution is wired before any refresh tick fires.
        try {
            HologramManager.enable();
        } catch (Throwable t) {
            logging.stack("Failed to enable the hologram subsystem — continuing without holograms", t);
        }

    }

    /**
     * Exposes the built-in void {@link ChunkGenerator} to the rest of the server. Users can
     * create void worlds with e.g. {@code /mv create hub normal -g EternalParkour} or by setting
     * {@code generator: EternalParkour} in bukkit.yml, without installing a separate generator plugin.
     *
     * <p>Returns a generator that produces empty chunks (no noise, no surface, no caves,
     * no bedrock, no decorations, no mobs, no structures). Same implementation EternalParkour uses
     * for its own parkour world.</p>
     *
     * @param worldName the name of the world being generated (informational; ignored)
     * @param id        the generator id token from {@code -g EternalParkour:<id>}; ignored — we
     *                  always produce a void world regardless of token.
     */
    @Override
    public ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return VoidGenerator.getGenerator();
    }

    @Override
    public void disable() {
        // Tear down the hologram subsystem FIRST. Its entities are setPersistent(false)
        // and won't survive in the world file even if we skip this, but doing it
        // explicitly here means the chunk tickets are released cleanly and a /reload
        // (which calls disable→enable on the same JVM) starts from a clean slate.
        try {
            HologramManager.disable();
        } catch (Throwable t) {
            logging.stack("Error while disabling the hologram subsystem", t);
        }

        // Every step runs even if an earlier one fails, so players always get their
        // inventory and location back and data is always saved.

        // Finish queued writes (e.g. inventory backups) first; from here on all I/O runs on this thread.
        runSafely("finishing pending data writes", DataIO::flush);

        // Restore players first. Urgent: the server is stopping, so async teleports
        // might never complete and their inventory would never be restored.
        for (ParkourUser user : ParkourUser.getUsers()) {
            runSafely("restoring " + user.getName(), () -> ParkourUser.unregister(user, true, false, true));
        }

        if (elytraEnabled) {
            runSafely("disabling elytra parkour", IEP.INSTANCE::disable);
            elytraEnabled = false;
        }

        if (plusEnabled) {
            runSafely("disabling the multiplayer modes", IPP::disable);
            plusEnabled = false;
        }

        runSafely("saving the default leaderboard", () -> {
            if (Modes.DEFAULT != null) {
                Modes.DEFAULT.getLeaderboard().write(false);
            }
        });

        runSafely("disabling server integrations", () -> {
            IntegrationLoader.disable(this, serverIntegrations);
            serverIntegrations = List.of();
        });

        runSafely("unregistering PlaceholderAPI expansions", () -> {
            if (placeholderHook != null) {
                placeholderHook.unregister();
                placeholderHook = null;
            }
            if (legacyPlaceholderHook != null) {
                legacyPlaceholderHook.unregister();
                legacyPlaceholderHook = null;
            }
        });

        runSafely("closing storage", Storage::close);
        runSafely("deleting the parkour world", World::delete);

        // Static state must never survive a disable (e.g. /reload) and leak sessions or players.
        Divider.sections.clear();
        Command.selections.clear();
    }

    private static boolean runSafely(String action, Runnable runnable) {
        try {
            runnable.run();
            return true;
        } catch (Throwable t) {
            logging.stack("Error while " + action, t);
            return false;
        }
    }
}
