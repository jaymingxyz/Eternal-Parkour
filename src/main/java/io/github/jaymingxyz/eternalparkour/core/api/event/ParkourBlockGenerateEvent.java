package io.github.jaymingxyz.eternalparkour.core.api.event;

import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWrapper;
import org.bukkit.block.Block;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourBlockGenerateEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final List<Block> blocks;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourBlockGenerateEvent(List<Block> blocks, ParkourGenerator generator, ParkourPlayer player) {
        this.blocks = blocks;
        this.generator = generator;
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
