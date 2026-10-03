package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.generator.SpeedDemonGenerator
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score.Companion.pretty

object SpeedDemonMode : Mode {

    override val name = "speed demon"

    override val leaderboard = Leaderboard(name)

    override fun getGenerator() = SpeedDemonGenerator()

    override fun formatDisplayScore(score: Double): String {
        return if (Config.CONFIG.getBoolean("settings.metric.default")) {
            "${(score * 3.6).pretty()} km/h"
        } else {
            "${(score * 2.23694).pretty()} mph"
        }
    }
}