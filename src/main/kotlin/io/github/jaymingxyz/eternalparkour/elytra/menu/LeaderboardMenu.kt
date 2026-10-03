package io.github.jaymingxyz.eternalparkour.elytra.menu

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.leaderboard.Leaderboard
import io.github.jaymingxyz.eternalparkour.elytra.mode.Mode
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.meta.SkullMeta

object LeaderboardMenu {

    fun open(player: Player) {
        val menu = MenuStyle.frame(Menu(3, Locales.getString(player, "leaderboards.title")))
            .distributeRowsEvenly()
            .item(22, Locales.getItem(player, "close").click({ player.closeInventory() }))

        for (mode in IEP.getModes()) {
            if (Config.CONFIG.getBoolean("permissions") && !player.hasPermission("iep.leaderboard.${mode.name}")) {
                continue
            }

            menu.item(menu.items.size + 9, MenuStyle.hint(mode.getItem(player), Locales.getString(player, "hints.view"))
                .click({ SingleLeaderboardMenu.open(player, mode, mode.leaderboard.sort) })
            )
        }

        menu.open(player.player)
    }
}

private object SingleLeaderboardMenu {

    // gold, silver and bronze for the top three
    private val MEDALS = listOf("<#FFD700><bold>", "<#C0C0C0><bold>", "<#CD7F32><bold>")

    fun open(player: Player, mode: Mode, sort: Leaderboard.Sort) {
        val leaderboard = mode.leaderboard
        val menu = MenuStyle.list(Locales.getString(player, "modes.${mode.name}.title"),
            Locales.getItem(player, "previous page"),
            Locales.getItem(player, "next page"),
            Locales.getItem(player, "empty"))
        val bar = MenuStyle.center(menu)

        for ((idx, entry) in sort.sort(leaderboard.getAllScores()).withIndex()) {
            val (uuid, score) = entry

            val item = Locales.getItem(player, "leaderboards.head", (idx + 1).toString(), score.name,
                mode.formatDisplayScore(score.score), score.getFormattedTime(), score.seed.toString())

            if (idx < MEDALS.size) {
                item.modifyName { MEDALS[idx] + Strings.stripTags(it) }
            }

            // prevent crashes from fetching all the skulls at once
            if (idx <= 36) {
                val meta = item.build().itemMeta
                (meta as SkullMeta).owningPlayer = Bukkit.getOfflinePlayer(uuid)
                item.meta(meta)
            }

            menu.addToDisplay(listOf(item))

            if (uuid == player.uniqueId) {
                menu.item(bar - 2, item.clone())
            }
        }

        val current = Locales.getStringList(player, "leaderboards.sort.values")
        val next = when (sort) {
            Leaderboard.Sort.SCORE -> Leaderboard.Sort.TIME
            Leaderboard.Sort.TIME -> Leaderboard.Sort.SCORE
        }

        menu
            .item(bar + 2, MenuStyle.hint(Locales.getItem(player, "leaderboards.sort", current[sort.ordinal]),
                Locales.getString(player, "hints.change"))
                .click({ open(player, mode, next) }))
            .item(bar, Locales.getItem(player, "go back").click({ LeaderboardMenu.open(player) }))
            .open(player.player)
    }
}
