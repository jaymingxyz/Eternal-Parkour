package io.github.jaymingxyz.eternalparkour.elytra.player

import io.github.jaymingxyz.eternalparkour.core.config.Config
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task
import io.github.jaymingxyz.eternalparkour.core.player.data.ParkourItems
import io.github.jaymingxyz.eternalparkour.core.player.data.PlayerBackup
import io.github.jaymingxyz.eternalparkour.core.player.data.PlayerBackups
import io.github.jaymingxyz.eternalparkour.elytra.IEP
import io.github.jaymingxyz.eternalparkour.elytra.config.Locales
import io.github.jaymingxyz.eternalparkour.elytra.mode.Mode
import io.github.jaymingxyz.eternalparkour.elytra.reward.Reward
import io.github.jaymingxyz.eternalparkour.elytra.world.World
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.util.Vector
import java.util.concurrent.CompletableFuture

/**
 * An elytra player's state from before they joined, given back when they leave. Also written to disk
 * as a [PlayerBackup] (when `options.inventory-saving` is on in config.yml), so the player's inventory
 * survives a crash; it used to exist only in memory.
 *
 * Equipment setters use the explicit Java methods (setChestplate, setContents): Kotlin refuses to
 * synthesize a property over a getter/setter pair whose nullability annotations disagree, which is
 * the case in Paper's PlayerInventory.
 *
 * Inventory mutations are forced onto the main thread via [onMain]. Paper's
 * `teleportAsync(...).thenRun(...)` continuation thread varies, and inventory operations on a
 * non-main thread are silently dropped.
 */
class PreviousData(private val player: Player) {

    val leaveRewards: MutableMap<Mode, MutableSet<Reward>> = mutableMapOf()

    private val backup: PlayerBackup = PlayerBackup.capture(player, true)

    /**
     * Whether this instance wrote the backup file. Only then is it deleted after restoring.
     */
    private var ownsBackupFile = false

    init {
        if (Config.CONFIG.getBoolean("options.inventory-saving")) {
            PlayerBackups.save(player.uniqueId, player.name, backup)
            ownsBackupFile = true
        }
    }

    /**
     * Teleports the player into the IEP world, then sets up their inventory + state.
     * The inventory setup is guaranteed to run on the main thread regardless of where
     * the teleport future completes — see class doc.
     */
    fun setup(vector: Vector): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()

        player.teleportAsync(vector.toLocation(World.world)).thenRun {
            onMain {
                applySetup()
                future.complete(true)
            }
        }

        return future
    }

    private fun applySetup() {
        player.gameMode = GameMode.ADVENTURE
        player.fallDistance = 0F

        player.resetPlayerTime()
        player.activePotionEffects.forEach { player.removePotionEffect(it.type) }

        player.foodLevel = 20
        player.saturation = 20F
        player.isFlying = false
        player.allowFlight = false

        // Wipe everything — main inventory, off-hand, AND armor slots. Inventory.clear()
        // does cover armor on Paper 26, but a few setups (e.g. CMI's "vanish kit", some
        // anti-cheat preload, GameStack inventory mirroring) re-populate the chestplate
        // on the next tick. Calling setHelmet/Chestplate/Leggings/Boots(null) explicitly
        // ensures the slot is empty at the exact moment we then setChestplate(elytra) —
        // no race window where something else can put a curse-of-binding item in first.
        player.inventory.clear()
        player.inventory.setHelmet(null)
        player.inventory.setChestplate(null)
        player.inventory.setLeggings(null)
        player.inventory.setBoots(null)

        // Tagged, so they are taken away if they ever end up outside parkour.
        val elytra = ParkourItems.tag(Item(Material.ELYTRA, "").unbreakable().build())
        player.inventory.setChestplate(elytra)

        val items = listOf(
            Locales.getItem(player, "hotbar.play").build(),
            Locales.getItem(player, "hotbar.settings").build(),
            Locales.getItem(player, "hotbar.leaderboards").build(),
            Locales.getItem(player, "hotbar.leave").build(),
        ).map { ParkourItems.tag(it) }

        Menu.getEvenlyDistributedSlots(items.size).forEachIndexed { index, slot ->
            player.inventory.setItem(slot, items[index])
        }
    }

    /**
     * Gives the player back their state.
     *
     * @param switchMode True when the player is only switching to another elytra mode. The backup file
     * is kept, since they are still playing.
     * @param urgent Whether to do it all right now (the player is quitting or the server is stopping).
     * @param teleport Whether to send the player back to where they were. False when another plugin
     * teleported them out of the elytra world: they stay where that plugin put them.
     */
    fun reset(switchMode: Boolean, urgent: Boolean, teleport: Boolean = true) {
        val location = backup.location

        if (switchMode || !teleport || location == null) {
            onMain { resetInner(switchMode) }
            return
        }
        if (urgent) {
            player.teleport(location)
            onMain { resetInner(false) }
            return
        }

        player.teleportAsync(location).thenRun {
            onMain { resetInner(false) }
        }
    }

    private fun resetInner(switchMode: Boolean) {
        try {
            backup.applyState(player)
        } catch (ex: Exception) {
            // keep the backup file: it's the only copy left
            IEP.logging.stack("Error while giving ${player.name} back their state; their backup was kept", ex)
            return
        }

        ParkourItems.strip(player)

        if (!switchMode && ownsBackupFile) {
            ownsBackupFile = false
            PlayerBackups.delete(player.uniqueId)
        }

        for ((mode, rewards) in leaveRewards) {
            rewards.forEach { it.execute(player, mode) }
        }
    }

    /**
     * Routes [r] to the main server thread. Use for any operation that touches the
     * player's inventory or world state — Paper's teleportAsync continuation runs on
     * whatever thread the chunk loader handed control back on, and inventory writes
     * from that thread are silently dropped.
     */
    private fun onMain(r: () -> Unit) {
        if (Bukkit.isPrimaryThread()) {
            r()
        } else {
            Task.create(IEP.instance).execute(Runnable { r() }).run()
        }
    }
}
