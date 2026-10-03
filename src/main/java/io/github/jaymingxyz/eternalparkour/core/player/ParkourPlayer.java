package io.github.jaymingxyz.eternalparkour.core.player;

import com.google.gson.annotations.Expose;
import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.generator.ParkourGenerator;
import io.github.jaymingxyz.eternalparkour.core.generator.Profile;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.data.PreviousData;
import io.github.jaymingxyz.eternalparkour.core.player.data.ParkourItems;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.storage.Storage;
import io.github.jaymingxyz.eternalparkour.core.world.Divider;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.Menu;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Colls;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Task;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Subclass of {@link ParkourUser}. This class is used for players who are actively playing Parkour in any (default) mode.
 *
 * @author Efnilite
 */
public class ParkourPlayer extends ParkourUser {

    public static final Map<String, OptionContainer> PLAYER_COLUMNS = new HashMap<>();

    static {
        PLAYER_COLUMNS.put("uuid", new OptionContainer(null, null));
        PLAYER_COLUMNS.put("style", new OptionContainer(ParkourOption.STYLES, (player, v) -> player.style = v));
        PLAYER_COLUMNS.put("blockLead", new OptionContainer(ParkourOption.LEADS, (player, v) -> player.blockLead = Integer.parseInt(v)));
        PLAYER_COLUMNS.put("useParticles", new OptionContainer(ParkourOption.PARTICLES, (player, v) -> player.particles = parseBoolean(v)));
        PLAYER_COLUMNS.put("useSpecial", new OptionContainer(ParkourOption.SPECIAL_BLOCKS, (player, v) -> player.useSpecialBlocks = parseBoolean(v)));
        PLAYER_COLUMNS.put("showFallMsg", new OptionContainer(ParkourOption.FALL_MESSAGE, (player, v) -> player.showFallMessage = parseBoolean(v)));
        PLAYER_COLUMNS.put("showScoreboard", new OptionContainer(ParkourOption.SCOREBOARD, (player, v) -> player.showScoreboard = parseBoolean(v)));
        PLAYER_COLUMNS.put("selectedTime", new OptionContainer(ParkourOption.TIME, (player, v) -> player.selectedTime = Integer.parseInt(v)));
        PLAYER_COLUMNS.put("collectedRewards", new OptionContainer(null, (player, v) -> {
            player.collectedRewards = new ArrayList<>();

            if (!v.isEmpty()) {
                player.collectedRewards.addAll(Arrays.stream(v.replaceAll("[ \\[\\]]", "").split(","))
                        .distinct()
                        .toList());
            }
        }));
        PLAYER_COLUMNS.put("locale", new OptionContainer(ParkourOption.LANG, (player, v) -> {
            player._locale = v;
            player.locale = v;
        }));
        PLAYER_COLUMNS.put("schematicDifficulty", new OptionContainer(ParkourOption.SCHEMATICS, (player, v) -> player.schematicDifficulty = Double.parseDouble(v)));
        PLAYER_COLUMNS.put("sound", new OptionContainer(ParkourOption.SOUND, (player, v) -> player.sound = parseBoolean(v)));
    }

    public @Expose Double schematicDifficulty;
    public @Expose Integer blockLead;
    public @Expose Boolean particles;
    public @Expose Boolean sound;
    public @Expose Boolean useSpecialBlocks;
    public @Expose Boolean showFallMessage;
    public @Expose Boolean showScoreboard;
    public @Expose Integer selectedTime;
    public @Expose String style;
    public @Expose String _locale;
    public @Expose List<String> collectedRewards;
    /**
     * Creates a new instance of a ParkourPlayer<br>
     * If you are using the API, please use {@link ParkourPlayer#register(Player, Session)} instead
     */
    public ParkourPlayer(@NotNull Player player, @NotNull Session session, @Nullable PreviousData previousData) {
        super(player, session, previousData);

        this._locale = locale;

        // generic player settings
        player.setFlying(false);
        player.setAllowFlight(false);
        player.setInvisible(false);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private static boolean parseBoolean(String string) {
        return string == null
                || string.equals("1") // for MySQL
                || string.equals("true"); // for disk
    }

    /**
     * @param player The player.
     * @return True when this player is a {@link ParkourPlayer}, false if not.
     */
    public static boolean isPlayer(@Nullable Player player) {
        return player != null && getUser(player) instanceof ParkourPlayer;
    }

    /**
     * @param player The player.
     * @return player as a {@link ParkourPlayer}, null if not found.
     */
    public static @Nullable ParkourPlayer getPlayer(@NotNull Player player) {
        return getUser(player) instanceof ParkourPlayer pp ? pp : null;
    }

    /**
     * @return List with all players.
     */
    public static List<ParkourPlayer> getPlayers() {
        return getUsers().stream()
                .filter(user -> user instanceof ParkourPlayer)
                .map(user -> (ParkourPlayer) user)
                .toList();
    }

    @Override
    public void unregister() {
        EternalParkour.log("Unregistering player %s".formatted(player.getName()));

        if (session.generator != null &&
                session.generator.getMode() != null &&
                session.generator.getMode() instanceof MultiMode mode) {
            mode.leave(player, session);
        }

        session.removePlayers(this);

        save(EternalParkour.getPlugin().isEnabled());
    }

    /**
     * Sets the user's settings. If an item is not included, the setting gets reset.
     *
     * @param settings The settings map.
     */
    public void setSettings(@NotNull Map<String, Object> settings) {
        for (String key : PLAYER_COLUMNS.keySet()) {
            Object value = settings.get(key);
            OptionContainer container = PLAYER_COLUMNS.get(key);

            if (container.consumer == null) {
                continue;
            }

            if (value == null || !Option.OPTIONS_ENABLED.getOrDefault(container.option, true)) {
                container.consumer.accept(this, Option.OPTIONS_DEFAULTS.getOrDefault(container.option, ""));
                continue;
            }

            container.consumer.accept(this, String.valueOf(value));
        }
    }

    /**
     * Forces this player's generator to match the settings of this player.
     */
    public void updateGeneratorSettings(ParkourGenerator generator) {
        Profile profile = generator.profile;

        profile.set("schematicDifficulty", schematicDifficulty.toString())
                .set("blockLead", blockLead.toString())
                .set("particles", particles.toString())
                .set("sound", sound.toString())
                .set("useSpecialBlocks", useSpecialBlocks.toString())
                .set("showFallMessage", showFallMessage.toString())
                .set("showScoreboard", showScoreboard.toString())
                .set("selectedTime", selectedTime.toString())
                .set("style", style);

        generator.overrideProfile();
    }

    /**
     * @return A copy of this player's settings, keyed like {@link #PLAYER_COLUMNS}, as accepted by {@link #setSettings(Map)}.
     */
    public @NotNull Map<String, Object> getSettings() {
        Map<String, Object> settings = new HashMap<>();

        settings.put("style", style);
        settings.put("blockLead", blockLead);
        settings.put("useParticles", particles);
        settings.put("useSpecial", useSpecialBlocks);
        settings.put("showFallMsg", showFallMessage);
        settings.put("showScoreboard", showScoreboard);
        settings.put("selectedTime", selectedTime);
        settings.put("collectedRewards", collectedRewards == null ? "" : String.join(",", collectedRewards));
        settings.put("locale", locale);
        settings.put("schematicDifficulty", schematicDifficulty);
        settings.put("sound", sound);

        return settings;
    }

    /**
     * Saves the player's settings. Must be called on the main thread.
     *
     * @param async Whether to write on the I/O thread.
     */
    public void save(boolean async) {
        Storage.writePlayer(this, async);
    }

    public void setup(Location to) {
        EternalParkour.log("Setting up player %s".formatted(player.getName()));

        if (to != null) {
            teleport(to);
        }

        player.setGameMode(GameMode.ADVENTURE);

        // -= Inventory =-
        if (Config.CONFIG.getBoolean("options.inventory-handling")) {
            Task.create(EternalParkour.getPlugin()).delay(5).execute(() -> {
                // Left (or was teleported out) in the meantime: their own inventory is already back.
                if (!isActive()) {
                    return;
                }

                EternalParkour.log("Setting up inventory for player %s".formatted(player.getName()));

                player.getInventory().clear();

                List<Item> items = new ArrayList<>();

                if (ParkourOption.PLAY.mayPerform(player)) items.add(Locales.getItem(locale, "play.item"));
                if (ParkourOption.COMMUNITY.mayPerform(player)) items.add(Locales.getItem(locale, "community.item"));
                if (ParkourOption.SETTINGS.mayPerform(player)) items.add(Locales.getItem(locale, "settings.item"));
                if (ParkourOption.LOBBY.mayPerform(player)) items.add(Locales.getItem(locale, "lobby.item"));

                if (ParkourOption.QUIT.mayPerform(player)) items.add(Locales.getItem(locale, "other.quit"));

                List<Integer> slots = Menu.getEvenlyDistributedSlots(items.size());
                Colls.range(0, items.size()).forEach(idx -> player.getInventory().setItem(slots.get(idx), ParkourItems.tag(items.get(idx).build())));
            }).run();
        } else {
            sendTranslated("other.customize");
        }
    }

    public record OptionContainer(ParkourOption option, BiConsumer<ParkourPlayer, String> consumer) {

    }
}