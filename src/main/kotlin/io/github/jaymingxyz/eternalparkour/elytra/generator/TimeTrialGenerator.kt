package io.github.jaymingxyz.eternalparkour.elytra.generator

import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score.Companion.pretty
import kotlin.math.min

class TimeTrialGenerator : Generator() {

    override fun getScore() = min(SCORE, super.getScore())

    override fun tick() {
        super.tick()

        val score = getScore()

        if (score >= SCORE) {
            reset(ResetReason.RESET)
            return
        }

        player.sendActionBar("${getProgressBar(score)} <reset><dark_gray>| <gray>${score.pretty()}/${SCORE.pretty()}")
    }

    private fun getProgressBar(t: Double): String {
        return (0..<ACTIONBAR_LENGTH)
            .map { if (it * INCREMENTS < t) return@map "<green><bold>|" else return@map "<reset><dark_gray>|" }
            .joinToString("") { it }
    }

    override fun reset(resetReason: ResetReason, regenerate: Boolean, s: Int, overrideSeedSettings: Boolean) {
        // todo dont register scores < 2500

        super.reset(resetReason, regenerate, SEED, true)
    }

    companion object {
        var SEED = 0
            private set
        var SCORE = 0.0
            private set
        var ACTIONBAR_LENGTH = 0
            private set
        var INCREMENTS = 0.0
            private set

        init {
            load()
        }

        /**
         * Reads the time trial settings from the config.
         */
        fun load() {
            SEED = Config.CONFIG.getInt("mode-settings.time-trial.seed") { it >= 0 }
            SCORE = Config.CONFIG.getDouble("mode-settings.time-trial.score") { it > 0 }
            ACTIONBAR_LENGTH = Config.CONFIG.getInt("mode-settings.time-trial.actionbar-length") { it > 0 }
            INCREMENTS = SCORE / ACTIONBAR_LENGTH.toDouble()
        }
    }
}