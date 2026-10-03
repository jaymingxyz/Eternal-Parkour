package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.generator.ObstacleGenerator
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard

object ObstacleMode : Mode {

    override val name = "obstacle"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = ObstacleGenerator()

}