package io.github.jaymingxyz.eternalparkour.core.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Score;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Local disk (json) storage manager. Files are written to a temporary file first and then moved into
 * place, so a crash while writing never leaves a half-written file behind.
 *
 * @since 5.0.0
 */
class StorageDisk {

    /**
     * Player setting keys (see {@code ParkourPlayer#PLAYER_COLUMNS}) and the JSON field names
     * Infinite Parkour used for them in {@code players/<uuid>.json}.
     */
    private static final Map<String, String> PLAYER_FIELDS = Map.ofEntries(
            Map.entry("style", "style"),
            Map.entry("blockLead", "blockLead"),
            Map.entry("useParticles", "particles"),
            Map.entry("useSpecial", "useSpecialBlocks"),
            Map.entry("showFallMsg", "showFallMessage"),
            Map.entry("showScoreboard", "showScoreboard"),
            Map.entry("selectedTime", "selectedTime"),
            Map.entry("collectedRewards", "collectedRewards"),
            Map.entry("locale", "_locale"),
            Map.entry("schematicDifficulty", "schematicDifficulty"),
            Map.entry("sound", "sound"));

    public static @NotNull Map<UUID, Score> readScores(@NotNull String mode) {
        File file = getLeaderboardFile(mode);

        if (!file.exists()) {
            return new HashMap<>();
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            LeaderboardContainer read = EternalParkour.getGson().fromJson(reader, LeaderboardContainer.class);

            if (read == null || read.serialized == null) {
                return new HashMap<>();
            }

            Map<UUID, Score> scores = new HashMap<>();
            read.serialized.forEach((uuid, score) -> {
                try {
                    scores.put(uuid, Score.fromString(score));
                } catch (RuntimeException ex) {
                    EternalParkour.logging().warn("Skipping invalid score of %s in leaderboard %s: %s".formatted(uuid, mode, score));
                }
            });

            return scores;
        } catch (Exception ex) {
            EternalParkour.logging().stack("Error while trying to read leaderboard file %s".formatted(mode), ex);
            return new HashMap<>();
        }
    }

    public static void writeScores(@NotNull String mode, @NotNull Map<UUID, Score> scores) {
        LeaderboardContainer container = new LeaderboardContainer();
        scores.forEach((uuid, score) -> container.serialized.put(uuid, score.toString()));

        try {
            writeAtomically(getLeaderboardFile(mode).toPath(), writer -> EternalParkour.getGson().toJson(container, writer));
        } catch (IOException ex) {
            EternalParkour.logging().stack("Error while trying to write to leaderboard file %s".formatted(mode), ex);
        }
    }

    private static File getLeaderboardFile(String mode) {
        return EternalParkour.getInFolder("leaderboards/%s.json".formatted(mode.toLowerCase()));
    }

    public static class LeaderboardContainer {
        @Expose
        public final Map<UUID, String> serialized = new LinkedHashMap<>();
    }

    public static @NotNull Map<String, Object> readPlayer(@NotNull UUID uuid) {
        Map<String, Object> settings = new HashMap<>();
        File file = getPlayerFile(uuid);

        if (!file.exists()) {
            return settings;
        }

        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (!element.isJsonObject()) {
                return settings;
            }

            JsonObject json = element.getAsJsonObject();
            PLAYER_FIELDS.forEach((key, field) -> {
                JsonElement value = json.get(field);
                if (value == null || value.isJsonNull()) {
                    return;
                }

                if (value.isJsonArray()) {
                    List<String> list = new ArrayList<>();
                    value.getAsJsonArray().forEach(item -> list.add(item.getAsString()));
                    settings.put(key, String.join(",", list));
                } else {
                    settings.put(key, value.getAsString());
                }
            });
        } catch (Exception ex) {
            // a corrupt file must not stop the player from joining; they get default settings
            EternalParkour.logging().stack("Error while trying to read disk data of %s, using default settings".formatted(uuid), ex);
        }

        return settings;
    }

    public static void writePlayer(@NotNull UUID uuid, @NotNull Map<String, Object> settings) {
        JsonObject json = new JsonObject();

        PLAYER_FIELDS.forEach((key, field) -> {
            Object value = settings.get(key);
            if (value == null) {
                return;
            }

            switch (value) {
                case Number number -> json.addProperty(field, number);
                case Boolean bool -> json.addProperty(field, bool);
                default -> {
                    if (key.equals("collectedRewards")) {
                        JsonArray array = new JsonArray();
                        Arrays.stream(value.toString().split(","))
                                .filter(reward -> !reward.isBlank())
                                .forEach(array::add);
                        json.add(field, array);
                    } else {
                        json.addProperty(field, value.toString());
                    }
                }
            }
        });

        try {
            writeAtomically(getPlayerFile(uuid).toPath(), writer -> EternalParkour.getGson().toJson(json, writer));
        } catch (IOException ex) {
            EternalParkour.logging().stack("Error while trying to write disk data of %s".formatted(uuid), ex);
        }
    }

    private static File getPlayerFile(UUID uuid) {
        return EternalParkour.getInFolder("players/%s.json".formatted(uuid));
    }

    private static void writeAtomically(Path target, WriterAction action) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");

        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            action.write(writer);
        }

        Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(Writer writer) throws IOException;
    }
}
