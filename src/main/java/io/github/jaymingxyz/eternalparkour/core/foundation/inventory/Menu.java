package io.github.jaymingxyz.eternalparkour.core.foundation.inventory;

import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Numbers;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Class for Menu handling.
 *
 * <p>Every menu is the {@link InventoryHolder} of its own inventory. One shared listener, registered by
 * {@link #init(Plugin)}, recognises menus through that holder. While a menu is open no item can be moved
 * into or out of it: clicks on menu slots only trigger the menu item, and shift-clicks, double-click
 * collecting and drags that would touch the menu are cancelled.</p>
 *
 * @author Efnilite
 */
@SuppressWarnings("unused")
public class Menu implements InventoryHolder {

    private static Plugin plugin;

    /**
     * Registers the shared menu listener. Call once on enable.
     *
     * @param pl The plugin.
     */
    public static void init(Plugin pl) {
        plugin = pl;
        Bukkit.getPluginManager().registerEvents(new MenuListener(), pl);
    }

    protected final int rows;
    protected final String title;
    protected final Map<Integer, MenuItem> items = new HashMap<>();
    protected final List<Integer> evenlyDistributedRows = new ArrayList<>();
    protected boolean deactivated = false;
    protected Player player;
    protected Material filler = null;
    protected final Map<Integer, Material> rowFillers = new HashMap<>();
    private Inventory inventory;

    public Menu(int rows, String name) {
        if (rows < 1 || rows > 6) {
            throw new IllegalArgumentException("Rows must be above 1 and below 6");
        }
        this.rows = rows;
        this.title = name;
    }

    /**
     * Returns a list of slot numbers that evenly distribute the amount of items in a row.
     *
     * @param amountInRow The amount of items in the row.
     * @return A list of slot numbers that ensure an equal distribution.
     */
    public static List<Integer> getEvenlyDistributedSlots(int amountInRow) {
        return switch (amountInRow) {
            case 0 -> Collections.emptyList();
            case 1 -> Collections.singletonList(4);
            case 2 -> List.of(3, 5);
            case 3 -> List.of(3, 4, 5);
            case 4 -> List.of(2, 3, 5, 6);
            case 5 -> List.of(2, 3, 4, 5, 6);
            case 6 -> List.of(1, 2, 3, 5, 6, 7);
            case 7 -> List.of(1, 2, 3, 4, 5, 6, 7);
            case 8 -> List.of(0, 1, 2, 3, 5, 6, 7, 8);
            default -> List.of(0, 1, 2, 3, 4, 5, 6, 7, 8);
        };
    }

    /**
     * Sets an item to a slot
     *
     * @param slot The slot
     * @param item The item
     * @return the instance of this class
     */
    public Menu item(int slot, MenuItem item) {
        if (slot >= rows * 9 || slot < 0) {
            throw new IllegalArgumentException("Slot %d is not in inventory".formatted(slot));
        }

        items.put(slot, item);
        return this;
    }

    /**
     * Sets a specific set of rows to be distributed evenly. The items will be distributed.
     * Starts from 0 and goes up to 5.
     *
     * @param rows The rows
     * @return the instance of this class
     */
    public Menu distributeRowEvenly(int... rows) {
        for (int row : rows) {
            if (row < 0 || row > 5) {
                throw new IllegalArgumentException("Rows must be above 1 and below 6");
            }
            evenlyDistributedRows.add(row);
        }
        return this;
    }

    /**
     * Will distribute all rows evenly.
     *
     * @return the instance of this class
     * @see #distributeRowEvenly(int...)
     */
    public Menu distributeRowsEvenly() {
        evenlyDistributedRows.addAll(Numbers.getFromZero(rows));
        return this;
    }

    /**
     * Fills the background with a specific item
     *
     * @param filler The background filler
     * @return the instance of this class
     */
    public Menu fillBackground(@NotNull Material filler) {
        this.filler = filler;
        return this;
    }

    /**
     * Fills the empty slots of one row with a specific item, instead of the background.
     *
     * @param row    The row, starting from 0.
     * @param filler The filler for that row.
     * @return the instance of this class
     */
    public Menu fillRow(int row, @NotNull Material filler) {
        if (row < 0 || row >= rows) {
            throw new IllegalArgumentException("Row %d is not in inventory".formatted(row));
        }
        rowFillers.put(row, filler);
        return this;
    }

    /**
     * @param slot An empty slot.
     * @return The item that fills it, or null to leave it empty.
     */
    protected @Nullable MenuItem fillerFor(int slot) {
        Material material = rowFillers.getOrDefault(slot / 9, filler);

        // no name or tooltip: it is only background
        return material == null ? null : new Item(material, "").hideTooltip();
    }

    /**
     * Updates a specific item
     *
     * @param slots The slots which are to be updated
     */
    public void updateItem(int... slots) {
        if (!isOpen()) {
            return;
        }

        for (int slot : slots) {
            MenuItem item = items.get(slot);
            inventory.setItem(slot, item == null ? null : item.build());
        }
    }

    /**
     * Updates all items in the inventory
     */
    public void update() {
        if (!isOpen()) {
            return;
        }

        inventory.clear();
        items.forEach((slot, item) -> inventory.setItem(slot, item.build()));
    }

    /**
     * Updates all items by calling {@link #update()} in the inventory periodically.
     *
     * @param tickInterval The amount of ticks to wait between calling {@link #update()}
     */
    public void update(int tickInterval) {
        if (tickInterval <= 0) {
            throw new IllegalArgumentException("Tick interval must be above 0");
        }

        Task.create(plugin)
                .repeat(tickInterval)
                .execute(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!isOpen()) {
                            cancel();
                            return;
                        }

                        update();
                    }
                })
                .run();
    }

    /**
     * Opens the menu for the player. This distributes items on the same row automatically if these rows are assigned to automatically distribute.
     *
     * @param player The player to open it to
     */
    public void open(Player player) {
        this.player = player;
        this.deactivated = false;
        this.inventory = Bukkit.createInventory(this, rows * 9, Strings.component(title));

        // Evenly distributed rows
        for (int row : evenlyDistributedRows) {
            int min = row * 9; // 0 * 9 = 0
            int max = min + 8; // 0 + 8 = slot 8
            Map<Integer, MenuItem> itemsInRow = new HashMap<>();

            for (int slot : items.keySet()) { // get all items in the specified row
                if (slot >= min && slot <= max) {
                    itemsInRow.put(slot, items.get(slot));
                }
            }

            if (itemsInRow.keySet().isEmpty()) {
                continue;
            }

            List<Integer> sortedSlots = itemsInRow.keySet().stream().sorted().toList(); // sort all slots
            List<Integer> slots = getEvenlyDistributedSlots(sortedSlots.size()); // evenly distribute items
            List<Integer> olds = new ArrayList<>();
            List<Integer> news = new ArrayList<>();

            for (int i = 0; i < slots.size(); i++) {
                int newSlot = slots.get(i) + (9 * row); // gets the new slot
                int oldSlot = sortedSlots.get(i); // the previous slot
                MenuItem item = itemsInRow.get(oldSlot); // the item in the previous slot

                news.add(newSlot);
                olds.add(oldSlot);
                items.put(newSlot, item); // put item in new slot
            }

            for (int oldSlot : olds) {
                if (news.contains(oldSlot)) {
                    continue;
                }
                items.remove(oldSlot); // remove items from previous slot without deleting ones that are to-be moved
            }
        }

        // Filler, ignoring already-set items
        for (int slot = 0; slot < rows * 9; slot++) {
            if (items.get(slot) != null) {
                continue;
            }

            MenuItem fillerItem = fillerFor(slot);
            if (fillerItem != null) {
                items.put(slot, fillerItem);
            }
        }

        items.forEach((slot, item) -> inventory.setItem(slot, item.build()));

        player.openInventory(inventory);
    }

    /**
     * @return True when this menu is currently open for its player.
     */
    public boolean isOpen() {
        return !deactivated
                && player != null
                && inventory != null
                && player.getOpenInventory().getTopInventory().getHolder(false) == this;
    }

    /**
     * Returns the item in the respective slot.
     *
     * @param slot The slot
     * @return the item in this slot. This may be null.
     */
    public @Nullable MenuItem getItem(int slot) {
        return items.get(slot);
    }

    /**
     * Gets the slots and their respective items
     *
     * @return a Map with the slots and items.
     */
    public Map<Integer, MenuItem> getItems() {
        return items;
    }

    /**
     * Gets the player
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * @return The number of rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return The title, as given to the constructor.
     */
    public String getTitle() {
        return title;
    }

    @Override
    public @NotNull Inventory getInventory() {
        if (inventory == null) {
            throw new IllegalStateException("Menu has not been opened yet");
        }
        return inventory;
    }

    /**
     * The one listener for every menu.
     */
    private static final class MenuListener implements Listener {

        @EventHandler(priority = EventPriority.LOW)
        public void click(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) {
                return;
            }

            // Nothing may enter or leave a menu, whatever the click.
            event.setCancelled(true);

            if (menu.deactivated || event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }

            MenuItem clickedItem = menu.items.get(event.getSlot());
            if (clickedItem == null) {
                return;
            }

            clickedItem.handleClick(menu, event, event.getClick());
        }

        @EventHandler(priority = EventPriority.LOW)
        public void drag(InventoryDragEvent event) {
            if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu)) {
                return;
            }

            int topSize = event.getView().getTopInventory().getSize();
            if (event.getRawSlots().stream().anyMatch(slot -> slot < topSize)) {
                event.setCancelled(true);
            }
        }

        @EventHandler
        public void close(InventoryCloseEvent event) {
            if (event.getInventory().getHolder(false) instanceof Menu menu) {
                menu.deactivated = true;
            }
        }
    }
}
