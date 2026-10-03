package io.github.jaymingxyz.eternalparkour.elytra.mode

import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.Generator
import io.github.jaymingxyz.eternalparkour.elytra.generator.section.PointType
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Score.Companion.pretty
import org.bukkit.entity.Player
import org.jetbrains.annotations.Contract

/**
 * Interface for all modes.
 * Every registered mode needs to inherit this class, because it needs identifying functions.
 */
interface Mode {

    /**
     * @return The internal name used for this mode.
     */
    val name: String

    /**
     * @return The [Leaderboard] that belongs to this mode
     */
    val leaderboard: Leaderboard

    /**
     * The [PointType].
     */
    val pointType
        get() = PointType.CIRCLE

    /**
     * Returns a new [Generator] for this mode.
     */
    @Contract(pure = true)
    fun getGenerator(): Generator

    /**
     * @param player The player.
     * @return The item used in menus to show this mode. If this item is null, the mode won't be displayed.
     */
    fun getItem(player: Player) = Locales.getItem(player, "modes.$name")

    /**
     * @param score The score to format.
     * @return The formatted score.
     */
    fun formatDisplayScore(score: Double): String = score.pretty()

}