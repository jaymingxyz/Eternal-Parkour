package io.github.jaymingxyz.eternalparkour.core.leaderboard;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.DataIO;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;
import io.github.jaymingxyz.eternalparkour.core.storage.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Class for handling leaderboards.
 *
 * <p>Scores are only changed on the main thread. After every change a sorted, immutable copy is published,
 * which is what all lookups use, so lookups are safe from any thread (for example PlaceholderAPI requests).
 * Storage reads and writes run on the I/O thread and never touch the live scores.</p>
 */
public class Leaderboard {

    /**
     * The mode that this leaderboard belongs to
     */
    public final String mode;

    /**
     * The way in which items will be sorted.
     */
    public final Sort sort;

    /**
     * All scores, sorted. Main thread only.
     */
    private final Map<UUID, Score> scores = new LinkedHashMap<>();

    /**
     * Players whose score was reset since the last write, so the database row can be deleted. Main thread only.
     */
    private final Set<UUID> removed = new HashSet<>();

    /**
     * Sorted copy of {@link #scores}, replaced after every change.
     */
    private volatile Ranking ranking = new Ranking(List.of(), Map.of());

    public Leaderboard(@NotNull String mode, Sort sort) {
        this.mode = mode.toLowerCase();
        this.sort = sort;

        Storage.init(mode);

        // Read synchronously: scores recorded before an async read completes would otherwise be wiped by it.
        replaceAll(Storage.readScores(this.mode));

        var interval = Config.CONFIG.getInt("storage-update-interval");

        // With joining enabled this server owns the scores and saves them periodically.
        // Without joining (a leaderboard-only server) it periodically reads them instead.
        Task.create(EternalParkour.getPlugin())
                .delay(interval * 20)
                .repeat(interval * 20)
                .execute(Config.CONFIG.getBoolean("joining") ? () -> {
                    EternalParkour.log("Periodic saving of leaderboard data of %s".formatted(mode));

                    write(true);
                } : () -> {
                    EternalParkour.log("Periodic reading of leaderboard data of %s".formatted(mode));

                    read(true);
                })
                .run();
    }

    /**
     * Writes all scores to storage. Must be called on the main thread.
     *
     * @param async Whether to write on the I/O thread.
     */
    public void write(boolean async) {
        EternalParkour.log("Saving leaderboard data of %s".formatted(mode));

        Map<UUID, Score> snapshot = new LinkedHashMap<>(scores);
        Set<UUID> removedSnapshot = Set.copyOf(removed);
        removed.clear();

        if (async) {
            DataIO.run(() -> Storage.writeScores(mode, snapshot, removedSnapshot));
        } else {
            Storage.writeScores(mode, snapshot, removedSnapshot);
        }
    }

    /**
     * Replaces all scores with the stored ones. Must be called on the main thread.
     *
     * @param async Whether to read on the I/O thread.
     */
    public void read(boolean async) {
        EternalParkour.log("Reading leaderboard data of %s".formatted(mode));

        if (async) {
            DataIO.run(() -> {
                Map<UUID, Score> read = Storage.readScores(mode);
                DataIO.sync(() -> replaceAll(read));
            });
        } else {
            replaceAll(Storage.readScores(mode));
        }
    }

    private void replaceAll(Map<UUID, Score> read) {
        scores.clear();
        scores.putAll(read);
        sort();
    }

    /**
     * Returns a sorted copy of the scores.
     *
     * @param sort The sorting method.
     * @return A sorted map of scores.
     */
    public Map<UUID, Score> sort(Sort sort) {
        LinkedHashMap<UUID, Score> sorted = new LinkedHashMap<>();

        ranking.entries().stream()
                .sorted(comparator(sort))
                .forEachOrdered(entry -> sorted.put(entry.getKey(), entry.getValue()));

        return sorted;
    }

    static Comparator<Map.Entry<UUID, Score>> comparator(Sort sort) {
        Comparator<Score> byTime = Comparator.comparingLong(Score::getTimeMillis);

        Comparator<Score> comparator = switch (sort) {
            case SCORE -> Comparator.comparingInt(Score::score).reversed().thenComparing(byTime);
            case TIME -> byTime;
            case DIFFICULTY -> Comparator.comparingDouble(Score::getDifficultyValue).reversed();
        };

        return Map.Entry.comparingByValue(comparator);
    }

    // sorts all scores and publishes the result
    private void sort() {
        List<Map.Entry<UUID, Score>> sorted = scores.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), entry.getValue()))
                .sorted(comparator(sort))
                .toList();

        scores.clear();
        Map<UUID, Integer> ranks = new HashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            var entry = sorted.get(i);
            scores.put(entry.getKey(), entry.getValue());
            ranks.put(entry.getKey(), i + 1);
        }

        ranking = new Ranking(sorted, Map.copyOf(ranks));
    }

    /**
     * Registers a new score, overriding the old one. Must be called on the main thread.
     *
     * @param uuid  The player's uuid
     * @param score The {@link Score} instance associated with a player's run
     * @return the previous score, if there was one
     */
    @Nullable
    public Score put(@NotNull UUID uuid, @NotNull Score score) {
        Score previous = scores.put(uuid, score);

        sort();

        return previous;
    }

    /**
     * Resets the score of a player by deleting it. Must be called on the main thread.
     *
     * @param uuid The UUID
     * @return the previous value if one was found
     */
    @Nullable
    public Score remove(@NotNull UUID uuid) {
        Score previous = scores.remove(uuid);
        removed.add(uuid);

        sort();

        return previous;
    }

    /**
     * Resets all registered scores for this mode. Must be called on the main thread.
     */
    public void resetAll() {
        removed.addAll(scores.keySet());
        scores.clear();

        sort();
    }

    /**
     * @param uuid The {@link UUID} to get.
     * @return The {@link Score} associated with the player. If none, returns {@link Score#EMPTY}.
     */
    @NotNull
    public Score get(@NotNull UUID uuid) {
        Ranking current = ranking;
        Integer rank = current.ranks().get(uuid);

        return rank == null ? Score.EMPTY : current.entries().get(rank - 1).getValue();
    }

    /**
     * @param uuid The uuid
     * @return The rank. Starts from 1. Returns 0 if no ranking is found.
     */
    public int getRank(@NotNull UUID uuid) {
        return ranking.ranks().getOrDefault(uuid, 0);
    }

    /**
     * Gets the score at a specified rank.
     * Ranks start at 1.
     *
     * @param rank The rank
     * @return the {@link Score} instance, null if one isn't found
     */
    @Nullable
    public Score getScoreAtRank(int rank) {
        List<Map.Entry<UUID, Score>> entries = ranking.entries();

        if (rank < 1 || rank > entries.size()) {
            return null;
        }

        return entries.get(rank - 1).getValue();
    }

    /**
     * @return The number of scores.
     */
    public int size() {
        return ranking.entries().size();
    }

    private record Ranking(List<Map.Entry<UUID, Score>> entries, Map<UUID, Integer> ranks) {
    }

    public enum Sort {
        SCORE, TIME, DIFFICULTY
    }
}
