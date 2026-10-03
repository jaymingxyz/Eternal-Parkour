package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.generator.MinSpeedGenerator
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard

object MinSpeedMode : Mode {

    override val name = "min speed"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = MinSpeedGenerator()
}