package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.generator.Generator
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.PointType
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard

object CloseMode : Mode {

    override val name = "close"

    override val leaderboard = Leaderboard(name)

    override val pointType = PointType.FLAT

    override fun getGenerator() = Generator()

}