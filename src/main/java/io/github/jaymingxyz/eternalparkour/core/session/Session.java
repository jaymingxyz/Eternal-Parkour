package io.github.jaymingxyz.eternalparkour.core.session;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourSpectator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.world.Divider;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * <p>A session is bound to a {@link Divider} section.
 * It manages all players, all spectators, visibility, the generator, etc.</p>
 * <p>Iteration 2.</p>
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class Session {

    /**
     * The spawn location of this session.
     */
    private Location spawnLocation;

    /**
     * The generator.
     */
    public ParkourGenerator generator;

    /**
     * The visibility of this session. Default public.
     */
    private Visibility visibility = Visibility.PUBLIC;

    /**
     * List of muted users. Read from the async chat thread.
     */
    private final List<ParkourUser> muted = new CopyOnWriteArrayList<>();

    /**
     * All users, in the order they joined, so the first player is the session owner.
     * Changed on the main thread only, but read from the async chat thread, so every access is synchronized on it.
     */
    private final Map<UUID, ParkourUser> users = new LinkedHashMap<>();

    /**
     * Function that takes the current session and returns whether new players should be accepted.
     */
    private Function<Session, Boolean> isAcceptingPlayers = session -> false;

    /**
     * Function that takes the current session and returns whether new spectators should be accepted.
     */
    private Function<Session, Boolean> isAcceptingSpectators = session -> session.visibility == Visibility.PUBLIC;

    /**
     * Creates a new session.
     *
     * @param generatorFunction     The generator function.
     * @param isAcceptingPlayers    The function that takes the current session and returns whether new players should be accepted.
     * @param isAcceptingSpectators The function that takes the current session and returns whether new spectators should be accepted.
     * @param players               The players.
     * @return The session.
     */
    public static Session create(Function<Session, ParkourGenerator> generatorFunction,
                                 Function<Session, Boolean> isAcceptingPlayers,
                                 Function<Session, Boolean> isAcceptingSpectators,
                                 Player... players) {
        EternalParkour.log("Creating session");

        if (players != null) {
            EternalParkour.log("Players in session: %s".formatted(Arrays.stream(players).map(Player::getName).toList()));
        }

        Session session = new Session();

        if (isAcceptingPlayers != null) session.isAcceptingPlayers = isAcceptingPlayers;
        if (isAcceptingSpectators != null) session.isAcceptingSpectators = isAcceptingSpectators;

        List<ParkourPlayer> pps = new ArrayList<>();
        if (players != null) {
            for (Player player : players) {
                ParkourPlayer pp = ParkourUser.register(player, session);
                session.addPlayers(pp);
                pps.add(pp);
            }
        }

        session.spawnLocation = Divider.add(session);
        session.generator = generatorFunction.apply(session);

        if (players != null) {
            pps.forEach(p -> p.updateGeneratorSettings(session.generator));
        }

        session.generator.island.build(session.spawnLocation);

        return session;
    }

    /**
     * Sets the visibility of this session.
     * @param visibility The visibility.
     */
    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /**
     * @return The visibility of this session.
     */
    public Visibility getVisibility() {
        return visibility;
    }

    /**
     * Adds provided players to this session's player list.
     *
     * @param toAdd The players to add.
     */
    public void addPlayers(ParkourPlayer... toAdd) {
        for (ParkourPlayer player : toAdd) {
            EternalParkour.log("Adding player %s to session".formatted(player.getName()));

            for (ParkourPlayer to : getPlayers()) {
                to.sendTranslated("lobby.other_join", player.getName());
            }

            synchronized (users) {
                users.put(player.getUUID(), player);
            }
            ParkourUser.index(player);
        }
    }

    /**
     * Removes provided players from this session's player list.
     *
     * @param toRemove The players to remove.
     */
    public void removePlayers(ParkourPlayer... toRemove) {
        for (ParkourPlayer player : toRemove) {
            EternalParkour.log("Removing player %s from session".formatted(player.getName()));

            synchronized (users) {
                users.remove(player.getUUID());
            }
            ParkourUser.unindex(player);
        }

        List<ParkourPlayer> players = getPlayers();
        for (ParkourPlayer player : toRemove) {
            for (ParkourPlayer to : players) {
                to.sendTranslated("lobby.other_leave", player.getName());
            }
        }

        if (toRemove.length > 0 && players.isEmpty()) {
            generator.reset(false);
            Divider.remove(this);
        }
    }

    /**
     * @return The players, in the order they joined. The first player is the session owner.
     */
    public List<ParkourPlayer> getPlayers() {
        synchronized (users) {
            return users.values().stream()
                    .filter(user -> user instanceof ParkourPlayer)
                    .map(user -> (ParkourPlayer) user)
                    .toList();
        }
    }

    /**
     * Adds provided spectators to this session's spectator list.
     *
     * @param spectators The spectators to add.
     */
    public void addSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            EternalParkour.log("Adding spectator %s to session".formatted(spectator.getName()));

            for (ParkourPlayer player : getPlayers()) {
                player.sendTranslated("play.spectator.other_join", spectator.getName());
            }

            synchronized (users) {
                users.put(spectator.getUUID(), spectator);
            }
            ParkourUser.index(spectator);
        }
    }

    /**
     * Removes provided spectators from this session's spectator list.
     *
     * @param spectators The spectators to remove.
     */
    public void removeSpectators(ParkourSpectator... spectators) {
        for (ParkourSpectator spectator : spectators) {
            EternalParkour.log("Removing spectator %s from session".formatted(spectator.getName()));

            for (ParkourPlayer player : getPlayers()) {
                player.sendTranslated("play.spectator.other_leave", spectator.getName());
            }

            synchronized (users) {
                users.remove(spectator.getUUID());
            }
            ParkourUser.unindex(spectator);
        }
    }

    /**
     * @return The spectators.
     */
    public List<ParkourSpectator> getSpectators() {
        synchronized (users) {
            return users.values().stream()
                    .filter(user -> user instanceof ParkourSpectator)
                    .map(user -> (ParkourSpectator) user)
                    .toList();
        }
    }

    /**
     * @return The users.
     */
    public List<ParkourUser> getUsers() {
        synchronized (users) {
            return new ArrayList<>(users.values());
        }
    }

    /**
     * @return the spawn location for this {@link Session}.
     */
    @SuppressWarnings("unused")
    public Location getSpawnLocation() {
        return spawnLocation.clone();
    }

    /**
     * Toggles mute for the specified user.
     *
     * @param user The user to (un)mute.
     */
    public void toggleMute(@NotNull ParkourUser user) {
        if (!muted.remove(user)) {
            muted.add(user);
        }
    }

    /**
     * @param user The user.
     * @return True when the user is muted, false if not.
     */
    public boolean isMuted(@NotNull ParkourUser user) {
        return muted.contains(user);
    }

    /**
     * @return True when players may join this session, false if not.
     */
    public boolean isAcceptingPlayers() {
        return isAcceptingPlayers.apply(this);
    }

    /**
     * @return True when spectators may join this session, false if not.
     */
    public boolean isAcceptingSpectators() {
        return isAcceptingSpectators.apply(this);
    }

    public enum Visibility {

        /**
         * No-one can join.
         */
        PRIVATE,

        /**
         * Only people with the session id can join.
         */
        ID_ONLY,

        /**
         * Anyone can join.
         */
        PUBLIC,

    }

    /**
     * An enum for all available chat types that a player can select while playing
     */
    public enum ChatType {

        LOBBY_ONLY, PLAYERS_ONLY, PUBLIC

    }
}