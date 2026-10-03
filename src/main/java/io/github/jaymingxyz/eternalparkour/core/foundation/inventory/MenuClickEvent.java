package io.github.jaymingxyz.eternalparkour.core.foundation.inventory;

import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Container class for clicking on menus
 */
public record MenuClickEvent(int slot, Menu menu, MenuItem item, InventoryClickEvent event) {

    public Player getPlayer() {
        return (Player) event.getWhoClicked();
    }
}