package io.github.jaymingxyz.eternalparkour.core.player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.api.event.ParkourJoinEvent;
import io.github.jaymingxyz.eternalparkour.core.api.event.ParkourLeaveEvent;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.hook.FloodgateHook;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.data.PreviousData;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.storage.Storage;
import io.github.jaymingxyz.eternalparkour.core.foundation.scoreboard.Sidebar;
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.ChannelNotRegisteredException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Superclass of every type of player. This encompasses every player currently in the Parkour world.
 * This includes active players ({@link ParkourPlayer}) and spectators ({@link ParkourSpectator}).
 *
 * @author Efnilite
 */
public abstract class ParkourUser {

    /**
     * Registers a player. This registers the player internally.
     * This automatically unregisters the player if it is already registered.
     *
     * @param player The player
     * @return the ParkourPlayer instance of the newly joined player
     */
    public static @NotNull ParkourPlayer register(@NotNull Player player, @NotNull Session session) {
        PreviousData data = null;
        ParkourUser existing = getUser(player);

        if (existing != null) {
            EternalParkour.log("Registering player %s with existing data".formatted(player.getName()));

            data = existing.previousData;
            unregister(existing, false, false, false);
        } else {
            EternalParkour.log("Registering player %s".formatted(player.getName()));
        }
        ParkourPlayer pp = new ParkourPlayer(player, session, data);

        // stats
        joinCount++;
        new ParkourJoinEvent(pp).call();

        Storage.readPlayer(pp);
        return pp;
    }

    /**
     * This is the same as {@link #leave(ParkourUser)}, but instead for a Bukkit player instance.
     *
     * @param player The Bukkit player instance that will be removed from the game if the player is active.
     * @see #leave(ParkourUser)
     */
    public static void leave(@NotNull Player player) {
        ParkourUser user = getUser(player);
        if (user == null) {
            return;
        }
        leave(user);
    }

    /**
     * Forces user to leave. Follows behaviour of /parkour leave.
     *
     * @param user The user.
     */
    public static void leave(@NotNull ParkourUser user) {
        unregister(user, true, true, false);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param user                The user to unregister.
     * @param restorePreviousData Whether to restore the data from before the player joined the parkour.
     * @param kickIfBungee        Whether to kick the player if Bungeecord mode is enabled.
     * @param urgent              Whether to restore right now (the player is quitting or the server is stopping).
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent) {
        unregister(user, restorePreviousData, kickIfBungee, urgent, true);
    }

    /**
     * Unregisters a Parkour user instance.
     *
     * @param user                The user to unregister.
     * @param restorePreviousData Whether to restore the data from before the player joined the parkour.
     * @param kickIfBungee        Whether to kick the player if Bungeecord mode is enabled.
     * @param urgent              Whether to restore right now (the player is quitting or the server is stopping).
     * @param teleportBack        Whether restoring also sends the player back to where they were. False when
     *                            another plugin teleported them out of the parkour world.
     */
    public static void unregister(@NotNull ParkourUser user, boolean restorePreviousData, boolean kickIfBungee, boolean urgent, boolean teleportBack) {
        new ParkourLeaveEvent(user).call();
        EternalParkour.log("Unregistering player %s, restorePreviousData = %s, kickIfBungee = %s".formatted(user.getName(), restorePreviousData, kickIfBungee));

        try {
            user.unregister();

            if (user.board != null && !user.board.isDeleted()) {
                user.board.delete();
            }
        } catch (Exception ex) { // safeguard to prevent people from losing data
            EternalParkour.logging().stack("Error while trying to make player %s leave".formatted(user.getName()), ex);
            user.send("<red><bold>There was an error while trying to handle leaving.");
        }

        if (restorePreviousData && Config.CONFIG.getBoolean("bungeecord.enabled") && kickIfBungee) {
            sendPlayerToServer(user.player, Config.CONFIG.getString("bungeecord.return_server"));
            return;
        }

        if (!restorePreviousData) return;

        user.previousData.apply(user.player, urgent, teleportBack);

        Mode mode = user.session.generator.getMode();
        if (mode == null) {
            EternalParkour.logging().error("Mode is null for %s".formatted(user.getName()));
            mode = Modes.DEFAULT;
        }

        if (user instanceof ParkourPlayer player) {
            Mode finalMode = mode;
            user.previousData.onLeave.forEach(r -> r.execute(player, finalMode));
        }
    }

    // Sends a player to a BungeeCord server. server is the server name.
    private static void sendPlayerToServer(Player player, String server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);

        try {
            player.sendPluginMessage(EternalParkour.getPlugin(), "BungeeCord", out.toByteArray());
        } catch (ChannelNotRegisteredException ex) {
            EternalParkour.logging().stack("Error while trying to send %s to server %s. This server is not registered.".formatted(player.getName(), server), ex);
            player.kick(Component.text("Couldn't move you to %s. Please rejoin.".formatted(server)));
        }
    }

    /**
     * Every user that is currently part of a session, by UUID. Maintained by {@link Session} on the main
     * thread, and safe to read from any thread (e.g. the async chat thread).
     */
    private static final Map<UUID, ParkourUser> USERS = new ConcurrentHashMap<>();

    /**
     * Called by {@link Session} when a user is added to it.
     */
    @ApiStatus.Internal
    public static void index(@NotNull ParkourUser user) {
        USERS.put(user.getUUID(), user);
    }

    /**
     * Called by {@link Session} when a user is removed from it. Does nothing if the player has since been
     * registered again as a different user.
     */
    @ApiStatus.Internal
    public static void unindex(@NotNull ParkourUser user) {
        USERS.remove(user.getUUID(), user);
    }

    /**
     * @param player The player.
     * @return True when this player is a {@link ParkourUser}, false if not.
     */
    public static boolean isUser(@Nullable Player player) {
        return player != null && USERS.containsKey(player.getUniqueId());
    }

    /**
     * @param player The player.
     * @return player as a {@link ParkourUser}, null if not found.
     */
    public static @Nullable ParkourUser getUser(@NotNull Player player) {
        return USERS.get(player.getUniqueId());
    }

    /**
     * @return Set with all users.
     */
    public static Set<ParkourUser> getUsers() {
        return new HashSet<>(USERS.values());
    }

    /**
     * This user's locale
     */
    @NotNull
    public String locale = Option.OPTIONS_DEFAULTS.get(ParkourOption.LANG);

    /**
     * This user's scoreboard
     */
    public Sidebar board;

    /**
     * This user's PreviousData
     */
    @NotNull
    public PreviousData previousData;

    /**
     * The selected {@link Session.ChatType}
     */
    public Session.ChatType chatType = Session.ChatType.PUBLIC;

    /**
     * The {@link Session} this user is in.
     */
    public final Session session;

    /**
     * The Bukkit player instance associated with this user.
     */
    public final Player player;

    /**
     * The {@link Instant} when the player joined.
     */
    public final Instant joined;

    /**
     * The amount of players that have joined while the plugin has been enabled.
     */
    public static int joinCount;

    public ParkourUser(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        this.player = player;
        this.session = session;
        this.joined = Instant.now();

        if (previousData == null) {
            leaveElytra(player);
        }
        this.previousData = previousData == null ? new PreviousData(player) : previousData;
    }

    /**
     * Makes a player leave elytra parkour right away, so their state from before elytra parkour is what
     * gets saved, not the elytra loadout.
     */
    private static void leaveElytra(Player player) {
        if (!EternalParkour.isElytraEnabled()) {
            return;
        }

        ElytraPlayer elytraPlayer = ElytraPlayer.Companion.asElytraPlayer(player);
        if (elytraPlayer != null) {
            elytraPlayer.leave(false, true, true);
        }
    }

    /**
     * Unregisters this user.
     */
    protected abstract void unregister();

    /**
     * Teleports the player asynchronously.
     *
     * @param to Where the player will be teleported to
     */
    public void teleport(@NotNull Location to) {
        player.teleportAsync(to);
    }

    /**
     * Sends a message.
     *
     * @param message The message
     */
    public void send(String message) {
        player.sendMessage(Strings.component(message));
    }

    /**
     * Sends a chat message from another player, formatted with a translated template.
     * The sender's message is inserted as-is and never parsed for MiniMessage tags.
     *
     * @param key     The translation key of the template. Its first {@code %s} is the sender name, the second the message.
     * @param sender  The sender's name.
     * @param message The chat message.
     */
    public void sendChat(String key, String sender, Component message) {
        String template = Locales.getString(locale, key).formatted("<ep_sender>", "<ep_message>");

        player.sendMessage(MiniMessage.miniMessage().deserialize(template,
                Placeholder.unparsed("ep_sender", sender),
                Placeholder.component("ep_message", message)));
    }

    /**
     * Sends a translated message
     *
     * @param key    The translation key
     * @param format Any objects that may be given to the formatting of the string.
     */
    public void sendTranslated(String key, Object... format) {
        send(Locales.getString(locale, key).formatted(format));
    }

    /**
     * Updates the scoreboard for the specified generator.
     *
     * @param generator The generator.
     */
    public void updateScoreboard(ParkourGenerator generator) {
        boolean shown = Boolean.parseBoolean(Option.OPTIONS_DEFAULTS.get(ParkourOption.SCOREBOARD))
                && generator.profile.get("showScoreboard").asBoolean();

        if (!shown) {
            if (board != null && !board.isDeleted()) {
                board.delete();
            }
            return;
        }

        // Created only when shown, since it replaces the player's scoreboard. Never for a user who
        // already left: a generator can tick once more after that.
        if (board == null || board.isDeleted()) {
            if (!isActive()) {
                return;
            }
            board = new Sidebar(player);
        }

        Leaderboard leaderboard = generator.getMode().getLeaderboard();
        Score top = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.getScoreAtRank(1);
        Score high = leaderboard == null ? new Score("?", "?", "?", 0) : leaderboard.get(getUUID());
        if (top == null) {
            top = new Score("?", "?", "?", 0);
        }

        board.updateTitle(replace(Locales.getString(locale, "scoreboard.title"), top, high, generator));
        board.updateLines(replace(Locales.getStringList(locale, "scoreboard.lines"), top, high, generator));
    }

    private List<String> replace(List<String> s, Score top, Score high, ParkourGenerator generator) {
        return s.stream().map(line -> replace(line, top, high, generator)).toList();
    }

    private String replace(String s, Score top, Score high, ParkourGenerator generator) {
        return Strings.colour(translate(player, s)
                .replace("%score%", Integer.toString(generator.score))
                .replace("%time%", generator.getFormattedTime())
                .replace("%difficulty%", Double.toString(generator.getDifficultyScore()))

                .replace("%top_score%", Integer.toString(top.score()))
                .replace("%top_player%", top.name())
                .replace("%top_time%", top.time())

                .replace("%high_score%", Integer.toString(high.score()))
                .replace("%high_score_time%", high.time()));
    }

    // translate papi
    private String translate(Player player, String string) {
        return EternalParkour.getPlaceholderHook() == null ? string : PlaceholderAPI.setPlaceholders(player, string);
    }

    /**
     * Delayed tasks must check this before changing the player: by the time they run, the player may have
     * left (and got their own inventory, effects and game mode back) or be playing as another user.
     *
     * @return True if this user is still the one registered for an online player.
     */
    public boolean isActive() {
        return player.isOnline() && getUser(player) == this;
    }

    /**
     * @return The player's uuid
     */
    public UUID getUUID() {
        return player.getUniqueId();
    }

    /**
     * @return The player's location
     */
    public Location getLocation() {
        return player.getLocation();
    }

    /**
     * @return The player's name
     */
    public String getName() {
        return player.getName();
    }

    /**
     * @param player The player
     * @return true if the player is a Bedrock player, false if not.
     */
    public static boolean isBedrockPlayer(Player player) {
        return Bukkit.getPluginManager().isPluginEnabled("floodgate") && FloodgateHook.isBedrockPlayer(player);
    }
}