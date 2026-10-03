package io.github.jaymingxyz.eternalparkour.core.leaderboard;

/**
 * Represents a record, used to keep track of the score a player may achieve.
 *
 * @param name       The name of the player
 * @param time       The time it took to achieve this score, as {@code minutes:seconds:millis}
 * @param difficulty The difficulty of this run
 * @param score      The score achieved
 */
public record Score(String name, String time, String difficulty, int score) {

    /**
     * Used for players without a score.
     */
    public static final Score EMPTY = new Score("?", "?", "?", 0);

    /**
     * Gets a {@link Score} instance from a string
     *
     * @param string The string
     * @return a {@link Score} instance based off the provided string
     */
    public static Score fromString(String string) {
        String[] parts = string.split(",");

        return new Score(parts[0], parts[1], parts[2], Integer.parseInt(parts[3]));
    }

    /**
     * Formats a duration the way {@link #time()} stores it. Minutes are not capped at 59.
     *
     * @param millis The duration in milliseconds.
     * @return The formatted duration.
     */
    public static String formatTime(long millis) {
        long minutes = millis / 60_000;
        long seconds = (millis / 1000) % 60;
        long ms = millis % 1000;

        return "%02d:%02d:%03d".formatted(minutes, seconds, ms);
    }

    /**
     * @return This score's time in millis, or {@link Long#MAX_VALUE} if the time is unknown, so unknown
     * times sort last.
     */
    public long getTimeMillis() {
        String[] split = time.split(":");

        try {
            long m = Long.parseLong(split[0]);
            long s = Long.parseLong(split[1]);
            long ms = Long.parseLong(split[2]);

            return m * 60 * 1000 + s * 1000 + ms;
        } catch (RuntimeException ex) {
            return Long.MAX_VALUE;
        }
    }

    /**
     * @return This score's difficulty, or -1 if it is unknown, so unknown difficulties sort last.
     */
    public double getDifficultyValue() {
        try {
            return Double.parseDouble(difficulty);
        } catch (RuntimeException ex) {
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s", name, time, difficulty, score);
    }
}
