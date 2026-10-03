package io.github.jaymingxyz.eternalparkour.elytra.reward

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Config

object Rewards {

    var enabled = false
        private set
    var scoreRewards: Map<Int, Set<Reward>> = emptyMap()
        private set
    var intervalRewards: Map<Int, Set<Reward>> = emptyMap()
        private set
    var oneTimeRewards: Map<Int, Set<Reward>> = emptyMap()
        private set

    init {
        load()
    }

    /**
     * Reads the rewards from rewards.yml.
     */
    fun load() {
        enabled = Config.REWARDS.getBoolean("enabled")
        scoreRewards = getRewards("score")
        intervalRewards = getRewards("interval")
        oneTimeRewards = getRewards("one-time")
    }

    private fun getRewards(path: String): Map<Int, Set<Reward>> {
        if (!enabled) {
            return emptyMap()
        }

        val rewards = mutableMapOf<Int, Set<Reward>>()
        for (score in Config.REWARDS.getPaths(path)) {
            val fullPath = "$path.$score"

            try {
                val parsedScore = score.toInt()

                if (parsedScore < 1) {
                    IEP.logging.error("Invalid score $score in rewards")
                    continue
                }

                rewards[parsedScore] = Config.REWARDS.getStringList(fullPath)
                    .map { Reward(it) }
                    .toSet()
            } catch (ex: NumberFormatException) {
                IEP.logging.error("Invalid score $score in rewards")
            }
        }

        return rewards
    }

}
