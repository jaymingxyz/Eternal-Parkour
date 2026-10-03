package io.github.jaymingxyz.eternalparkour.core.player.data;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.elytra.player.ElytraPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Items the plugin hands out during parkour (hotbar menu items, the elytra, the duels start item) are
 * tagged, and taken away from anyone who has one while not playing. They must never end up in the
 * survival game: some are valuable (an unbreakable elytra) or unobtainable (a barrier block).
 *
 * <p>Normally parkour replaces the whole inventory on leave, so tagged items only get out when that
 * doesn't happen, e.g. after a crash, or when another plugin moves an item out of a playing player's
 * inventory (an auction house, a trade command). Tagged items are removed when a player joins, every
 * few seconds, and whenever someone not playing tries to pick one up or place one.</p>
 */
public final class ParkourItems implements Listener {

    private static final int SCAN_INTERVAL_TICKS = 5 * 20;

    private static NamespacedKey key;

    private ParkourItems() {
    }

    /**
     * Registers the listener and the periodic scan.
     *
     * @param plugin The plugin.
     */
    public static void init(@NotNull Plugin plugin) {
        key = new NamespacedKey(plugin, "parkour_item");

        Bukkit.getPluginManager().registerEvents(new ParkourItems(), plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isPlaying(player)) {
                    strip(player);
                }
            }
        }, SCAN_INTERVAL_TICKS, SCAN_INTERVAL_TICKS);
    }

    /**
     * Marks an item as handed out by parkour.
     *
     * @param item The item.
     * @return The same item, for chaining.
     */
    public static @NotNull ItemStack tag(@NotNull ItemStack item) {
        item.editPersistentDataContainer(pdc -> pdc.set(key, PersistentDataType.BYTE, (byte) 1));
        return item;
    }

    /**
     * @param item The item.
     * @return True when the item was handed out by parkour.
     */
    public static boolean isTagged(@Nullable ItemStack item) {
        return key != null && item != null && !item.isEmpty() && item.getPersistentDataContainer().has(key);
    }

    /**
     * Removes every tagged item from a player's inventory, armour, off hand and cursor.
     *
     * @param player The player.
     * @return The number of stacks removed.
     */
    public static int strip(@NotNull Player player) {
        int removed = 0;

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (isTagged(contents[slot])) {
                inventory.setItem(slot, null);
                removed++;
            }
        }

        if (isTagged(player.getItemOnCursor())) {
            player.setItemOnCursor(null);
            removed++;
        }

        if (removed > 0) {
            EternalParkour.log("Removed %d parkour items from %s".formatted(removed, player.getName()));
        }
        return removed;
    }

    /**
     * @param player The player.
     * @return True when the player is in block parkour (incl. spectating) or elytra parkour.
     */
    public static boolean isPlaying(@NotNull Player player) {
        if (ParkourUser.isUser(player)) {
            return true;
        }
        return EternalParkour.isElytraEnabled() && ElytraPlayer.Companion.asElytraPlayer(player) != null;
    }

    // After the crash restore, which runs at LOWEST.
    @EventHandler(priority = EventPriority.NORMAL)
    public void join(PlayerJoinEvent event) {
        if (!isPlaying(event.getPlayer())) {
            strip(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void pickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player && isTagged(event.getItem().getItemStack()) && !isPlaying(player)) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void place(BlockPlaceEvent event) {
        if (isTagged(event.getItemInHand()) && !isPlaying(event.getPlayer())) {
            event.setCancelled(true);
            strip(event.getPlayer());
        }
    }
}
