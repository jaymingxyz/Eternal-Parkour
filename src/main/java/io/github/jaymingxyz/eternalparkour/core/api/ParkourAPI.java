package io.github.jaymingxyz.eternalparkour.core.api;

import io.github.jaymingxyz.eternalparkour.core.api.event.ParkourBlockGenerateEvent;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Main API handler.
 * <ul>
 *     <li>For general player handling, please view {@link ParkourUser}.</li>
 *     <li>For parkour players, please view {@link ParkourPlayer}.</li>
 *     <li>For player spectating, please view {@link io.github.jaymingxyz.eternalparkour.core.player.ParkourSpectator}.</li>
 *     <li>For events, please view {@link ParkourBlockGenerateEvent} and others in the events package.</li>
 *     <li>For Sessions, please view {@link io.github.jaymingxyz.eternalparkour.core.session.Session}.</li>
 *     <li>For Schematics, please view {@link io.github.jaymingxyz.eternalparkour.core.schematic.Schematic}</li>
 * </ul>
 */
@SuppressWarnings("unused")
public class ParkourAPI {

    private ParkourAPI() throws IllegalAccessException {
        throw new IllegalAccessException("Initializing API class");
    }

    /**
     * @param player The player.
     * @return The {@link ParkourPlayer}. Null if not found.
     */
    public static @Nullable ParkourPlayer getPlayer(Player player) {
        return ParkourPlayer.getPlayer(player);
    }

    /**
     * @param player The player.
     * @return The {@link ParkourPlayer}. Null if not found.
     */
    public static @Nullable ParkourUser getUser(Player player) {
        return ParkourUser.getUser(player);
    }
}
