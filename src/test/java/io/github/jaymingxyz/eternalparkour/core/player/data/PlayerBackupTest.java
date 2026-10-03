package io.github.jaymingxyz.eternalparkour.core.player.data;

import org.bukkit.GameMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PlayerBackupTest {

    // Items and effects need a running server to (de)serialize, so they stay empty here.
    @Test
    void testJsonRoundTrip() {
        UUID worldId = UUID.randomUUID();
        PlayerBackup backup = new PlayerBackup(GameMode.SURVIVAL, worldId, "world",
                12.5, 64.0, -3.25, 90f, -10f, 17, 3.5f, true, true, List.of(), null);

        var json = backup.toJson();
        PlayerBackup read = PlayerBackup.fromJson(json);

        json.remove("created");
        var reread = read.toJson();
        reread.remove("created");
        assertEquals(json, reread);
        assertFalse(read.hasItems());
        assertEquals(0, read.countItems());
        assertEquals("SURVIVAL", json.get("gameMode").getAsString());
        assertEquals(worldId.toString(), json.getAsJsonObject("location").get("worldId").getAsString());
        assertEquals(1, json.get("version").getAsInt());
    }
}
