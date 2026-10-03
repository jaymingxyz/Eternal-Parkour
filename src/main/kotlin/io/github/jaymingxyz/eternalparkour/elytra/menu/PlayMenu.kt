package io.github.jaymingxyz.eternalparkour.elytra.menu

import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer.Companion.asElytraPlayer
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu
import io.github.jaymingxyz.eternalparkour.core.menu.MenuStyle
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Cooldowns
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser
import org.bukkit.entity.Player

object PlayMenu {

    fun open(player: Player) {
        val menu = MenuStyle.frame(Menu(3, Locales.getString(player, "play.title")))
            .item(22, Locales.getItem(player, "close").click({ player.closeInventory() }))
            .distributeRowsEvenly()

        for (mode in IEP.getModes()) {
            menu.item(9 + menu.items.size, MenuStyle.hint(mode.getItem(player), Locales.getString(player, "hints.play"))
                .click({
                    if (!Cooldowns.canPerform(player, "ep join", 1000)) {
                        return@click
                    }

                    val join = {
                        val ep = player.asElytraPlayer()

                        if (ep == null) {
                            ElytraPlayer(player).join(mode)
                        } else {
                            ep.leave(true)

                            ep.join(mode)
                        }
                    }

                    if (ParkourUser.isUser(player)) {
                        IEP.log("Player is in block parkour, leaving it before joining elytra mode")

                        ParkourUser.leave(player)

                        Task.create(IEP.instance)
                            .delay(1)
                            .execute { if (player.isOnline && !ParkourUser.isUser(player)) join() }
                            .run()
                    } else {
                        join()
                    }
                }))
        }

        menu.open(player)
    }

}