package io.github.jaymingxyz.eternalparkour.core.leaderboard;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardSortTest {

    private static List<String> sortedNames(Leaderboard.Sort sort, Score... scores) {
        return java.util.Arrays.stream(scores)
                .map(score -> Map.entry(UUID.randomUUID(), score))
                .sorted(Leaderboard.comparator(sort))
                .map(entry -> entry.getValue().name())
                .toList();
    }

    @Test
    void testScoreSortsHighestFirstThenFastest() {
        var result = sortedNames(Leaderboard.Sort.SCORE,
                new Score("low", "00:10:000", "0.5", 5),
                new Score("slow", "02:00:000", "0.5", 10),
                new Score("fast", "01:00:000", "0.5", 10));

        assertEquals(List.of("fast", "slow", "low"), result);
    }

    @Test
    void testTimeSortsFastestFirstAndUnknownLast() {
        var result = sortedNames(Leaderboard.Sort.TIME,
                new Score("unknown", "?", "?", 1),
                new Score("hour", "61:00:000", "0.5", 1),
                new Score("minute", "01:00:000", "0.5", 1));

        assertEquals(List.of("minute", "hour", "unknown"), result);
    }

    @Test
    void testDifficultySortHandlesUnknownValues() {
        var result = sortedNames(Leaderboard.Sort.DIFFICULTY,
                new Score("unknown", "?", "?", 1),
                new Score("easy", "00:01:000", "0.2", 1),
                new Score("hard", "00:01:000", "1.0", 1));

        assertEquals(List.of("hard", "easy", "unknown"), result);
    }
}
