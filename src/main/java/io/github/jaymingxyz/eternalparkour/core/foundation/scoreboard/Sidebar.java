package io.github.jaymingxyz.eternalparkour.core.foundation.scoreboard;

import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * A per-player sidebar built on Paper's scoreboard API, with no packet or NMS access.
 *
 * <p>While a sidebar is shown the player gets their own {@link Scoreboard}. The scoreboard the player had
 * before is put back by {@link #delete()}. Lines are rendered with {@link org.bukkit.scoreboard.Score#customName}
 * and scores are hidden with {@link NumberFormat#blank()}, so lines can contain any text and are never
 * sorted by the client.</p>
 *
 * <p>Must only be used from the main thread.</p>
 */
public final class Sidebar {

    /**
     * The maximum number of lines the client shows.
     */
    public static final int MAX_LINES = 15;

    private static final String OBJECTIVE = "ep_sidebar";

    private final Player player;
    private final Scoreboard previous;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Component[] lines = new Component[MAX_LINES];

    private Component title = Component.empty();
    private int lineCount = 0;
    private boolean deleted = false;

    public Sidebar(@NotNull Player player) {
        this.player = player;

        // If the player still has another sidebar's scoreboard (for example when switching between block and
        // elytra parkour), don't restore to it later: it will be dead by then.
        Scoreboard current = player.getScoreboard();
        this.previous = current.getObjective(OBJECTIVE) != null ? Bukkit.getScoreboardManager().getMainScoreboard() : current;

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective(OBJECTIVE, Criteria.DUMMY, title);

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.numberFormat(NumberFormat.blank());

        player.setScoreboard(scoreboard);
    }

    /**
     * Updates the title.
     *
     * @param title The title as a legacy (section sign) string, as produced by {@link Strings#colour(String)}.
     */
    public void updateTitle(@NotNull String title) {
        updateTitle(Strings.fromLegacy(title));
    }

    /**
     * Updates the title.
     *
     * @param title The title.
     */
    public void updateTitle(@NotNull Component title) {
        checkDeleted();
        if (title.equals(this.title)) {
            return;
        }

        this.title = title;
        objective.displayName(title);
    }

    /**
     * Updates the lines.
     *
     * @param lines The lines as legacy (section sign) strings, as produced by {@link Strings#colour(String)}.
     */
    public void updateLines(@NotNull List<String> lines) {
        updateComponentLines(lines.stream().map(Strings::fromLegacy).toList());
    }

    /**
     * Updates the lines. Only lines that changed are sent to the player. Lines past {@link #MAX_LINES} are ignored.
     *
     * @param lines The lines.
     */
    public void updateComponentLines(@NotNull List<Component> lines) {
        checkDeleted();

        int count = Math.min(lines.size(), MAX_LINES);
        for (int i = 0; i < count; i++) {
            Component line = lines.get(i);
            // the score decides the order: the first line has the highest score
            boolean positionChanged = i >= lineCount || count != lineCount;

            if (positionChanged || !Objects.equals(this.lines[i], line)) {
                var score = objective.getScore(entry(i));
                score.setScore(count - i);
                score.customName(line);
                this.lines[i] = line;
            }
        }

        for (int i = count; i < lineCount; i++) {
            scoreboard.resetScores(entry(i));
            this.lines[i] = null;
        }

        lineCount = count;
    }

    /**
     * Removes the sidebar and gives the player back the scoreboard they had before.
     */
    public void delete() {
        if (deleted) {
            return;
        }
        deleted = true;

        try {
            objective.unregister();
        } catch (IllegalStateException ignored) {
            // already unregistered
        }

        if (player.isOnline() && player.getScoreboard() == scoreboard) {
            player.setScoreboard(previous != null ? previous : Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /**
     * @return True when this sidebar has been deleted.
     */
    public boolean isDeleted() {
        return deleted;
    }

    private void checkDeleted() {
        if (deleted) {
            throw new IllegalStateException("Sidebar is deleted");
        }
    }

    // score holder names; never shown because every score has a custom name
    private static String entry(int index) {
        return "ep_line_" + index;
    }
}
