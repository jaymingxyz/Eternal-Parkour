package io.github.jaymingxyz.eternalparkour.core.api.event;

import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWrapper;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Gets called when a new jump is generated. Read-only.
 *
 * @author Efnilite
 * @since 5.0.0
 */
public class ParkourSchematicGenerateEvent extends EventWrapper {

    private static final HandlerList HANDLERS = new HandlerList();

    public final Schematic schematic;
    public final ParkourGenerator generator;
    public final ParkourPlayer player;

    public ParkourSchematicGenerateEvent(Schematic schematic, ParkourGenerator generator, ParkourPlayer player) {
        this.schematic = schematic;
        this.generator = generator;
        this.player = player;
    }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
