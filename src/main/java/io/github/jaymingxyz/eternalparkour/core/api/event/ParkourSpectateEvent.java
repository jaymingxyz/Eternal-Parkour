package io.github.jaymingxyz.eternalparkour.core.api.event;

import io.github.jaymingxyz.eternalparkour.core.player.ParkourSpectator;
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWrapper;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a player starts spectating a session. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourSpectateEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourSpectator player;

    public ParkourSpectateEvent(ParkourSpectator player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
