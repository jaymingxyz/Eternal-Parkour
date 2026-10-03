package io.github.jaymingxyz.eternalparkour.core.mode;

import io.github.jaymingxyz.eternalparkour.core.session.Session;
import org.bukkit.entity.Player;

/**
 * Class for modes featuring multiple players.
 */
public interface MultiMode extends Mode {

    /**
     * Adds a player to this mode.
     *
     * @param player The player
     */
    void join(Player player, Session session);

    /**
     * Removes a player from this mode.
     *
     * @param player The player
     */
    void leave(Player player, Session session);

    /**
     * @return The max player count.
     */
    int getMaxPlayers();
}