package io.github.jaymingxyz.eternalparkour.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * MySQL storage manager.
 *
 * <p>Uses a small connection pool, so it can be used from the I/O thread and during startup at the same time.
 * All values are passed as statement parameters. Table names can't be parameters, so they are checked
 * against {@link #IDENTIFIER} instead.</p>
 *
 * @since 5.0.0
 */
class StorageSQL {

    /**
     * What a table name (prefix + mode name) may contain.
     */
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z0-9_\\-]+");

    private static HikariDataSource dataSource;

    /**
     * Creates the leaderboard table of a mode, connecting first if needed.
     */
    public static void init(String mode) {
        if (!ensureConnected()) {
            return;
        }

        update("""
                CREATE TABLE IF NOT EXISTS `%s`
                (
                    uuid       CHAR(36) NOT NULL PRIMARY KEY,
                    name       VARCHAR(16),
                    time       VARCHAR(16),
                    difficulty VARCHAR(3),
                    score      INT
                )
                CHARSET = utf8mb4 ENGINE = InnoDB;
                """.formatted(getTableName(mode)));
    }

    public static void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
            EternalParkour.log("Closed connection to MySQL");
        }
    }

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        Map<UUID, Score> scores = new HashMap<>();
        if (dataSource == null) {
            return scores;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, name, time, difficulty, score FROM `%s`".formatted(getTableName(mode)));
             ResultSet results = statement.executeQuery()) {

            while (results.next()) {
                try {
                    scores.put(UUID.fromString(results.getString("uuid")), new Score(
                            results.getString("name"),
                            results.getString("time"),
                            results.getString("difficulty"),
                            results.getInt("score")));
                } catch (IllegalArgumentException ex) {
                    EternalParkour.logging().warn("Skipping score with invalid UUID %s in %s".formatted(results.getString("uuid"), getTableName(mode)));
                }
            }
        } catch (SQLException ex) {
            EternalParkour.logging().stack("Error while trying to read SQL data of %s".formatted(mode), ex);
        }

        return scores;
    }

    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores, @NotNull Collection<UUID> removed) {
        if (dataSource == null) {
            return;
        }

        String table = getTableName(mode);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM `%s` WHERE uuid = ?".formatted(table));
                 PreparedStatement upsert = connection.prepareStatement("""
                         INSERT INTO `%s` (uuid, name, time, difficulty, score)
                         VALUES (?, ?, ?, ?, ?)
                         ON DUPLICATE KEY UPDATE name       = VALUES(name),
                                                 time       = VALUES(time),
                                                 difficulty = VALUES(difficulty),
                                                 score      = VALUES(score)
                         """.formatted(table))) {

                for (UUID uuid : removed) {
                    if (scores.containsKey(uuid)) {
                        continue; // scored again after being reset
                    }
                    delete.setString(1, uuid.toString());
                    delete.addBatch();
                }
                delete.executeBatch();

                for (Map.Entry<UUID, Score> entry : scores.entrySet()) {
                    Score score = entry.getValue();
                    upsert.setString(1, entry.getKey().toString());
                    upsert.setString(2, score.name());
                    upsert.setString(3, score.time());
                    upsert.setString(4, score.difficulty());
                    upsert.setInt(5, score.score());
                    upsert.addBatch();
                }
                upsert.executeBatch();

                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        } catch (SQLException ex) {
            EternalParkour.logging().stack("Error while trying to write SQL data of %s".formatted(mode), ex);
        }
    }

    // returns leaderboard table name
    private static String getTableName(String mode) {
        String name = "%sleaderboard-%s".formatted(Option.SQL_PREFIX, mode);

        if (!IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid table name %s: the SQL prefix and mode names may only contain letters, digits, _ and -".formatted(name));
        }
        return name;
    }

    private static String getOptionsTable() {
        String name = Option.SQL_PREFIX + "options";

        if (!IDENTIFIER.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid table name %s: the SQL prefix may only contain letters, digits, _ and -".formatted(name));
        }
        return name;
    }

    public static @NotNull Map<String, Object> readPlayer(@NotNull UUID uuid) {
        Map<String, Object> settings = new HashMap<>();
        if (dataSource == null) {
            return settings;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM `%s` WHERE uuid = ?".formatted(getOptionsTable()))) {
            statement.setString(1, uuid.toString());

            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return settings;
                }

                ResultSetMetaData meta = results.getMetaData();
                for (int column = 1; column <= meta.getColumnCount(); column++) {
                    settings.put(meta.getColumnLabel(column), results.getObject(column));
                }
            }
        } catch (SQLException ex) {
            EternalParkour.logging().stack("Error while trying to read SQL data of %s".formatted(uuid), ex);
        }

        return settings;
    }

    public static void writePlayer(@NotNull UUID uuid, @NotNull Map<String, Object> settings) {
        if (dataSource == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO `%s`
                     (uuid, style, blockLead, useParticles, useSpecial, showFallMsg, showScoreboard,
                      selectedTime, collectedRewards, locale, schematicDifficulty, sound)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE style               = VALUES(style),
                                             blockLead           = VALUES(blockLead),
                                             useParticles        = VALUES(useParticles),
                                             useSpecial          = VALUES(useSpecial),
                                             showFallMsg         = VALUES(showFallMsg),
                                             showScoreboard      = VALUES(showScoreboard),
                                             selectedTime        = VALUES(selectedTime),
                                             collectedRewards    = VALUES(collectedRewards),
                                             locale              = VALUES(locale),
                                             schematicDifficulty = VALUES(schematicDifficulty),
                                             sound               = VALUES(sound)
                     """.formatted(getOptionsTable()))) {

            statement.setString(1, uuid.toString());
            statement.setObject(2, settings.get("style"));
            statement.setObject(3, settings.get("blockLead"));
            statement.setObject(4, settings.get("useParticles"));
            statement.setObject(5, settings.get("useSpecial"));
            statement.setObject(6, settings.get("showFallMsg"));
            statement.setObject(7, settings.get("showScoreboard"));
            statement.setObject(8, settings.get("selectedTime") == null ? 0 : settings.get("selectedTime"));
            statement.setObject(9, settings.get("collectedRewards"));
            statement.setObject(10, settings.get("locale"));
            statement.setObject(11, settings.get("schematicDifficulty"));
            statement.setObject(12, settings.get("sound"));

            statement.executeUpdate();
        } catch (SQLException ex) {
            EternalParkour.logging().stack("Error while trying to write SQL data of %s".formatted(uuid), ex);
        }
    }

    /**
     * Connects if not connected yet. On failure the plugin is disabled, since running without the
     * configured database would lose data.
     *
     * @return True when connected.
     */
    private static synchronized boolean ensureConnected() {
        if (dataSource != null) {
            return true;
        }

        try {
            EternalParkour.log("Connecting to MySQL");

            HikariConfig config = new HikariConfig();
            config.setPoolName("EternalParkour-MySQL");
            config.setJdbcUrl("jdbc:mysql://%s:%d/%s".formatted(Option.SQL_URL, Option.SQL_PORT, Option.SQL_DB));
            config.setUsername(Option.SQL_USERNAME);
            config.setPassword(Option.SQL_PASSWORD);
            config.setMaximumPoolSize(4);
            config.setMinimumIdle(1);
            config.setConnectionTimeout(10_000);
            config.addDataSourceProperty("createDatabaseIfNotExist", "true");
            config.addDataSourceProperty("allowPublicKeyRetrieval", "true");
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("characterEncoding", "utf8");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");

            dataSource = new HikariDataSource(config);

            update("CREATE TABLE IF NOT EXISTS `%s` (`uuid` CHAR(36) NOT NULL, `style` VARCHAR(32), `blockLead` INT, `useParticles` BOOLEAN, `useSpecial` BOOLEAN, `showFallMsg` BOOLEAN, `showScoreboard` BOOLEAN, PRIMARY KEY (`uuid`)) ENGINE = InnoDB CHARSET = utf8mb4;".formatted(getOptionsTable()));

            // columns added by later Infinite Parkour versions; these fail harmlessly when already applied
            String options = getOptionsTable();
            updateSuppressed("ALTER TABLE `%s` DROP COLUMN `time`;".formatted(options)); // v3.0.0
            updateSuppressed("ALTER TABLE `%s` ADD `selectedTime` INT NOT NULL DEFAULT 0;".formatted(options));
            updateSuppressed("ALTER TABLE `%s` ADD `collectedRewards` MEDIUMTEXT;".formatted(options)); // v3.1.0
            updateSuppressed("ALTER TABLE `%s` ADD `locale` VARCHAR(8);".formatted(options)); // v3.6.0
            updateSuppressed("ALTER TABLE `%s` ADD `schematicDifficulty` DOUBLE;".formatted(options));
            updateSuppressed("ALTER TABLE `%s` ADD `sound` BOOLEAN;".formatted(options)); // v4.0.0
            updateSuppressed("ALTER TABLE `%s` DROP COLUMN `useDifficulty`;".formatted(options)); // 5.0.0
            updateSuppressed("ALTER TABLE `%s` DROP COLUMN `useStructure`;".formatted(options));

            EternalParkour.log("Connected to MySQL");
            return true;
        } catch (Exception ex) {
            EternalParkour.logging().stack("Could not connect to MySQL", "check your SQL settings in the config", ex);
            close();
            Bukkit.getPluginManager().disablePlugin(EternalParkour.getPlugin()); // data handling without db will go horribly wrong
            return false;
        }
    }

    private static void update(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException ex) {
            EternalParkour.logging().stack("Error while executing %s".formatted(sql), ex);
        }
    }

    // if the statement throws an error, ignore it
    private static void updateSuppressed(String sql) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException ignored) {
        }
    }
}
