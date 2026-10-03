package io.github.jaymingxyz.eternalparkour.elytra.leaderboard

import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class Score(val name: String, val score: Double, val time: Long, val seed: Int) {

    /**
     * @return The time in a formatted manner.
     */
    fun getFormattedTime(): String {
        return timeFormatter.format(Instant.ofEpochMilli(time))
    }

    companion object {

        var timeFormatter: DateTimeFormatter = createTimeFormatter()
            private set

        /**
         * Reads the time format from the config.
         */
        fun load() {
            timeFormatter = createTimeFormatter()
        }

        private fun createTimeFormatter() = DateTimeFormatter.ofPattern(Config.CONFIG.getString("time-format"))
            .withZone(ZoneOffset.UTC)

        fun Double.pretty(digits: Int = 1) = "%.${digits}f".format(this)

    }
}