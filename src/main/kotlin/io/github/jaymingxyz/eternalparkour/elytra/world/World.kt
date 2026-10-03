package io.github.jaymingxyz.eternalparkour.elytra.world

import io.github.jaymingxyz.eternalparkour.core.foundation.util.Worlds
import io.github.jaymingxyz.eternalparkour.elytra.IEP
import org.bukkit.World

/**
 * The elytra parkour world. It is always recreated on start and deleted on stop, because
 * its course is only ever sent to clients and never needs to be kept.
 */
object World {

    private const val NAME = "iep"

    lateinit var world: World

    /**
     * Creates the world, deleting any copy left behind by a previous run.
     */
    fun create() {
        IEP.log("Creating world $NAME")

        world = Worlds.createVoidWorld(IEP.instance, NAME, true)
            ?: throw IllegalStateException("Elytra world '$NAME' could not be created")

        world.time = 10_000

        // The elytra world has no mobs and the course is client-side, so a low simulation
        // distance saves random-tick work without affecting what players see.
        try {
            world.simulationDistance = 4
        } catch (_: Throwable) {
        }
    }

    /**
     * Unloads and deletes the world.
     */
    fun delete() {
        IEP.log("Deleting world $NAME")

        Worlds.deleteWorld(IEP.instance, if (::world.isInitialized) world else null, NAME)
    }
}
