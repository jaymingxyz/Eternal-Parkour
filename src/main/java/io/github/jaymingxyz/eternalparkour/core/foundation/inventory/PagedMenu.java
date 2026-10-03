package io.github.jaymingxyz.eternalparkour.core.foundation.inventory;

import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.MenuItem;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Numbers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A menu which contains multiple pages.
 *
 * <p>Page buttons given as an {@link Item} are templates: {@code %page%} and {@code %pages%} in their name
 * and lore become the page number and the number of pages, and clicking them turns the page.</p>
 *
 * @author Efnilite
 */
public class PagedMenu extends Menu {

    private int total;
    private int current;
    private int nextPageSlot;
    private MenuItem nextPageItem;
    private int prevPageSlot;
    private MenuItem prevPageItem;
    private MenuItem emptyItem;
    private final List<Integer> displaySlots = new ArrayList<>();
    private final List<MenuItem> totalToDisplay = new ArrayList<>();
    private final Map<Integer, List<MenuItem>> assigned = new HashMap<>();

    public PagedMenu(int rows, String name) {
        super(rows, name);
    }

    /**
     * Excludes a specific set of rows from being used to display items
     *
     * @param rows The rows which will be excluded. Starts from 0.
     * @return the instance of this
     */
    public PagedMenu displayRows(int... rows) {
        int begin = Integer.MAX_VALUE;
        int end = Integer.MIN_VALUE;

        for (int row : rows) {
            if (row < 0 || row > 5) {
                throw new IllegalArgumentException("Row must be above 0 and below 6!");
            }

            int min = row * 9;
            int max = min + 8;

            begin = Numbers.min(begin, min);
            end = Numbers.max(end, max);
        }
        displaySlots.addAll(Numbers.getFromTo(begin, end));
        return this;
    }

    /**
     * Shows the entries in columns 1 to 7 of these rows, leaving a border around them.
     *
     * @param firstRow The first row, starting from 0.
     * @param lastRow  The last row.
     * @return the instance of this
     */
    public PagedMenu displayArea(int firstRow, int lastRow) {
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = 1; column <= 7; column++) {
                displaySlots.add(row * 9 + column);
            }
        }
        return this;
    }

    @Override
    public void open(Player player) {
        assignPages();

        page(0);

        if (totalToDisplay.isEmpty() && emptyItem != null && !displaySlots.isEmpty()) {
            int middleRow = (displaySlots.getFirst() / 9 + displaySlots.getLast() / 9) / 2;
            items.put(middleRow * 9 + 4, emptyItem);
        }

        super.open(player);
    }

    public void page(int delta) {
        int newPage = current + delta;
        if (newPage < 0 || newPage > total) {
            return;
        }
        if (!assigned.containsKey(newPage) || assigned.get(newPage) == null) {
            return;
        }

        List<MenuItem> values = new ArrayList<>(assigned.get(newPage));

        items.remove(prevPageSlot);
        items.remove(nextPageSlot);

        if (newPage > 0 && prevPageItem != null) {
            items.put(prevPageSlot, pageButton(prevPageItem, newPage, -1));
        } else {
            putFiller(prevPageSlot);
        }
        if (newPage < total - 1 && nextPageItem != null) {
            items.put(nextPageSlot, pageButton(nextPageItem, newPage, 1));
        } else {
            putFiller(nextPageSlot);
        }

        for (int slot : displaySlots) {
            items.remove(slot);
            putFiller(slot);
        }

        // Fill row by row. A row that isn't full is centred, like the rest of the menus.
        Map<Integer, List<Integer>> rows = new LinkedHashMap<>();
        displaySlots.forEach(slot -> rows.computeIfAbsent(slot / 9, row -> new ArrayList<>()).add(slot));

        for (Map.Entry<Integer, List<Integer>> row : rows.entrySet()) {
            if (values.isEmpty()) {
                break;
            }

            List<Integer> slots = row.getValue();
            if (values.size() < slots.size()) {
                slots = getEvenlyDistributedSlots(values.size()).stream().map(column -> row.getKey() * 9 + column).toList();
            }

            for (int slot : slots) {
                items.put(slot, values.removeFirst());
            }
        }
        current = newPage;
        if (delta != 0) {
            update();
        }
    }

    private void putFiller(int slot) {
        MenuItem fillerItem = fillerFor(slot);
        if (fillerItem != null) {
            items.put(slot, fillerItem);
        }
    }

    private MenuItem pageButton(MenuItem button, int page, int delta) {
        if (!(button instanceof Item template)) {
            return button;
        }

        return template.clone()
                .modifyName(text -> pageText(text, page))
                .modifyLore(text -> pageText(text, page))
                .click(event -> page(delta));
    }

    private String pageText(String text, int page) {
        return text.replace("%pages%", Integer.toString(total)).replace("%page%", Integer.toString(page + 1));
    }

    private void assignPages() {
        List<MenuItem> total = new ArrayList<>(totalToDisplay);
        List<MenuItem> thisPage = new ArrayList<>();

        assigned.clear();
        int page = 0;
        while (!total.isEmpty()) {
            for (int slot = 0; slot < displaySlots.size(); slot++) {
                if (total.isEmpty()) {
                    break;
                }
                thisPage.add(total.getFirst());
                total.removeFirst();
            }

            this.assigned.put(page, new ArrayList<>(thisPage));
            thisPage.clear();
            page++;
        }

        this.current = 0;
        this.total = assigned.keySet().size();
    }

    public PagedMenu addToDisplay(List<MenuItem> items) {
        totalToDisplay.addAll(items);
        return this;
    }

    public void setToDisplay(List<MenuItem> items) {
        totalToDisplay.clear();
        totalToDisplay.addAll(items);
    }

    public PagedMenu nextPage(int slot, MenuItem item) {
        this.nextPageSlot = slot;
        this.nextPageItem = item;
        return this;
    }

    public PagedMenu prevPage(int slot, MenuItem item) {
        this.prevPageSlot = slot;
        this.prevPageItem = item;
        return this;
    }

    /**
     * Sets both page buttons. They only show when there is a page in their direction.
     *
     * @param prevSlot The slot of the previous page button.
     * @param nextSlot The slot of the next page button.
     * @param prev     The previous page button template.
     * @param next     The next page button template.
     * @return the instance of this
     */
    public PagedMenu pageButtons(int prevSlot, int nextSlot, Item prev, Item next) {
        return prevPage(prevSlot, prev).nextPage(nextSlot, next);
    }

    /**
     * @param item Shown in the middle of the list when there is nothing to show, or null for nothing.
     * @return the instance of this
     */
    public PagedMenu whenEmpty(@Nullable MenuItem item) {
        this.emptyItem = item;
        return this;
    }

    public List<MenuItem> getTotalToDisplay() {
        return totalToDisplay;
    }
}
