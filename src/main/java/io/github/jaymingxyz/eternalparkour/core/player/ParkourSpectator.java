package io.github.jaymingxyz.eternalparkour.core.player;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.api.event.ParkourSpectateEvent;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.player.data.PreviousData;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/**
 * Class for spectators of a Session.
 *
 * @author Efnilite
 */
public class ParkourSpectator extends ParkourUser {

    private final BukkitTask closestChecker;
    /**
     * The closest player.
     */
    @NotNull
    public ParkourPlayer closest;

    public ParkourSpectator(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        List<ParkourPlayer> players = session.getPlayers();
        if (players.isEmpty()) {
            throw new IllegalArgumentException("Can't spectate a session without players");
        }
        this.closest = players.get(0);

        new ParkourSpectateEvent(this).call();

        Task.create(EternalParkour.getPlugin())
            .delay(1)
            .execute(() -> {
                if (!isActive()) {
                    return;
                }

                teleport(closest.getLocation());

                sendTranslated("play.spectator.join");

                player.setGameMode(GameMode.SPECTATOR);
                player.setAllowFlight(true);
                player.setFlying(true);
                if (ParkourUser.isBedrockPlayer(player)) {  // bedrock has no spectator mode, so just make the player invisible
                    player.setInvisible(true);
                    player.setCollidable(false);
                }
            })
            .run();

        // on the main thread: it reads player locations and session state
        closestChecker = Task.create(EternalParkour.getPlugin())
            .delay(1)
            .repeat(10)
            .execute(() -> {
                if (session.getPlayers().isEmpty()) {
                    return;
                }

                closest = session.getPlayers().stream()
                        .min(Comparator.comparing(other -> other.getLocation().distanceSquared(player.getLocation()))) // x or x^2 doesn't matter in getting smallest
                        .orElse(closest);
            })
            .run();
    }

    /**
     * Updates the spectator's action bar, scoreboard and checks distance.
     */
    public void update() {
        player.sendActionBar(MiniMessage.miniMessage().deserialize(Locales.getString(player, "play.spectator.action_bar")));
        player.setGameMode(GameMode.SPECTATOR);
        updateScoreboard(session.generator);

        // spectator is still being teleported to world
        if (closest.getLocation().getWorld() != player.getLocation().getWorld()) {
            return;
        }

        if (closest.getLocation().distanceSquared(player.getLocation()) < 100 * 100) { // avoid sqrt
            return;
        }

        teleport(closest.getLocation());
    }

    /**
     * Stops the closest checker runnable.
     */
    @Override
    public void unregister() {
        closestChecker.cancel();
        session.removeSpectators(this);
        player.setInvisible(false);
    }
}