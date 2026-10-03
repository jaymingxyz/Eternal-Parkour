package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.generator.TimeTrialGenerator
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard

object TimeTrialMode : Mode {

    override val name = "time trial"

    override val leaderboard = Leaderboard(
        name,
        Config.CONFIG.getDouble("mode-settings.time-trial.score"),
        Leaderboard.Sort.TIME
    )

    override fun getGenerator() = TimeTrialGenerator()
}