package io.github.jaymingxyz.eternalparkour.core;

import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreTest {

    private Score score;

    @BeforeEach
    void setUp() {
        score = new Score("player", "00:00:00", "1.0", 100);
    }

    @Test
    void testFromString() {
        var result = Score.fromString("player,00:00:00,1.0,100");

        assertEquals(score, result);
    }

    @Test
    void testGetTimeMillis() {
        assertEquals(0, score.getTimeMillis());
        assertEquals(61_001, new Score("p", "01:01:001", "1.0", 1).getTimeMillis());
    }

    @Test
    void testUnknownValuesSortLast() {
        assertEquals(Long.MAX_VALUE, Score.EMPTY.getTimeMillis());
        assertEquals(-1, Score.EMPTY.getDifficultyValue());
    }

    @Test
    void testFormatTimeDoesNotWrapAfterAnHour() {
        assertEquals("00:00:000", Score.formatTime(0));
        assertEquals("01:02:003", Score.formatTime(62_003));
        assertEquals("75:00:500", Score.formatTime(75 * 60_000 + 500));

        long millis = 75 * 60_000 + 500;
        assertEquals(millis, new Score("p", Score.formatTime(millis), "1.0", 1).getTimeMillis());
    }

    @Test
    void testToString() {
        var result = score.toString();

        assertEquals("player,00:00:00,1.0,100", result);
    }
}
