package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.generator.Generator
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard

object DefaultMode : Mode {

    override val name = "default"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = Generator()
}