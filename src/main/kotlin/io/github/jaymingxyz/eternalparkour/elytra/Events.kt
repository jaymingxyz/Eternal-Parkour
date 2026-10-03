package io.github.jaymingxyz.eternalparkour.elytra

import io.github.jaymingxyz.eternalparkour.elytra.config.Config
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.generator.ResetReason
import io.github.jaymingxyz.eternalparkour.elytra.menu.LeaderboardMenu
import io.github.jaymingxyz.eternalparkour.elytra.menu.PlayMenu
import io.github.jaymingxyz.eternalparkour.elytra.menu.SettingsMenu
import io.github.jaymingxyz.eternalparkour.elytra.mode.DefaultMode
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer.Companion.asElytraPlayer
import io.github.jaymingxyz.eternalparkour.elytra.world.World
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWatcher
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot

object Events : EventWatcher {

    @EventHandler
    fun drop(event: PlayerDropItemEvent) {
        val player = event.player.asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun quit(event: PlayerQuitEvent) {
        val player = event.player.asElytraPlayer() ?: return

        player.leave(urgent = true)
    }

    @EventHandler
    fun change(event: PlayerChangedWorldEvent) {
        val player = event.player.asElytraPlayer() ?: return

        if (event.player.world == World.world) return

        // Another plugin teleported them out: give their things back, but leave them where they are.
        player.leave(teleport = false)
    }

    @EventHandler
    fun join(event: PlayerJoinEvent) {
        if (Config.CONFIG.getBoolean("join-on-join")) {
            ElytraPlayer(event.player).join(DefaultMode)
        }
    }

    @EventHandler
    fun rightRocket(event: BlockPlaceEvent) {
        val player = event.player.asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun inventoryClick(event: InventoryClickEvent) {
        if (event.whoClicked !is Player) return
        if (event.whoClicked.openInventory.type == InventoryType.CRAFTING) return
        val player = (event.whoClicked as Player).asElytraPlayer() ?: return

        event.isCancelled = true
    }

    @EventHandler
    fun damage(event: EntityDamageEvent) {
        if (event.entity !is Player) return

        val player = (event.entity as Player).asElytraPlayer() ?: return

        event.isCancelled = true

        if (event.cause == EntityDamageEvent.DamageCause.VOID) {
            player.getGenerator().reset(ResetReason.BOUNDS)
        }
    }

    @EventHandler
    fun rightSettings(event: PlayerInteractEvent) {
        if ((event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) ||
            event.hand != EquipmentSlot.HAND) return

        val player = event.player.asElytraPlayer() ?: return

        val play = Locales.getString(player, "hotbar.play.material").lowercase()
        val settings = Locales.getString(player, "hotbar.settings.material").lowercase()
        val leaderboard = Locales.getString(player, "hotbar.leaderboards.material").lowercase()
        val leave = Locales.getString(player, "hotbar.leave.material").lowercase()

        when (event.item?.type?.name?.lowercase()) {
            play -> {
                if (player.hasPermission("iep.play")) {
                    PlayMenu.open(player.player)
                }
            }
            settings -> {
                if (player.hasPermission("iep.setting")) {
                    SettingsMenu.open(player)
                }
            }
            leaderboard -> {
                if (player.hasPermission("iep.leaderboard")) {
                    LeaderboardMenu.open(player.player)
                }
            }
            leave -> {
                if (player.hasPermission("iep.leave")) {
                    player.leave()
                }
            }

            else -> {}
        }
    }
}