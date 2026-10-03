package io.github.jaymingxyz.eternalparkour.core.api.event;

import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWrapper;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a player falls. Read-only.
 */
public class ParkourFallEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final ParkourPlayer player;

    public ParkourFallEvent(ParkourPlayer player) {
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
