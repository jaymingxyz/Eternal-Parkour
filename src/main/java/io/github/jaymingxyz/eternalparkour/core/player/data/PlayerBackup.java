package io.github.jaymingxyz.eternalparkour.core.player.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Everything about a player that parkour changes, captured before they join so it can be given back:
 * gamemode, location, hunger, flight, potion effects and (optionally) the inventory.
 *
 * <p>Backups are also written to disk (see {@link PlayerBackups}), so the state survives a crash.
 * Items are stored in Paper's NBT format, which Minecraft's data fixers migrate between versions.</p>
 */
public final class PlayerBackup {

    private static final int VERSION = 1;

    private final GameMode gameMode;
    private final @Nullable UUID worldId;
    private final @Nullable String worldName;
    private final double x, y, z;
    private final float yaw, pitch;
    private final int foodLevel;
    private final float saturation;
    private final boolean allowFlight;
    private final boolean flying;
    private final List<PotionEffect> effects;
    private final @Nullable ItemStack[] items;

    // package-private for tests
    PlayerBackup(GameMode gameMode, @Nullable UUID worldId, @Nullable String worldName,
                         double x, double y, double z, float yaw, float pitch,
                         int foodLevel, float saturation, boolean allowFlight, boolean flying,
                         List<PotionEffect> effects, @Nullable ItemStack[] items) {
        this.gameMode = gameMode;
        this.worldId = worldId;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.allowFlight = allowFlight;
        this.flying = flying;
        this.effects = effects;
        this.items = items;
    }

    /**
     * Captures a player's current state. Must be called on the main thread.
     *
     * @param player       The player.
     * @param includeItems Whether to also capture the inventory (only when parkour will replace it).
     * @return The backup.
     */
    public static @NotNull PlayerBackup capture(@NotNull Player player, boolean includeItems) {
        Location location = player.getLocation();
        World world = location.getWorld();

        ItemStack[] items = null;
        if (includeItems) {
            ItemStack[] contents = player.getInventory().getContents();
            items = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                items[i] = contents[i] == null ? null : contents[i].clone();
            }
        }

        return new PlayerBackup(player.getGameMode(),
                world == null ? null : world.getUID(), world == null ? null : world.getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(),
                player.getFoodLevel(), player.getSaturation(), player.getAllowFlight(), player.isFlying(),
                new ArrayList<>(player.getActivePotionEffects()), items);
    }

    /**
     * @return The captured location, or null if its world isn't loaded anymore.
     */
    public @Nullable Location getLocation() {
        World world = worldId == null ? null : Bukkit.getWorld(worldId);
        if (world == null && worldName != null) {
            world = Bukkit.getWorld(worldName);
        }
        return world == null ? null : new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * @return True when this backup contains an inventory.
     */
    public boolean hasItems() {
        return items != null;
    }

    /**
     * @return The number of non-empty stacks in the inventory.
     */
    public int countItems() {
        int count = 0;
        if (items != null) {
            for (ItemStack item : items) {
                if (item != null && !item.isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Gives the player back everything except their location. Must be called on the main thread.
     *
     * @param player The player.
     */
    public void applyState(@NotNull Player player) {
        player.setGameMode(gameMode);
        player.setFoodLevel(foodLevel);
        player.setSaturation(saturation);
        player.setAllowFlight(allowFlight);
        player.setFlying(allowFlight && flying);

        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.addPotionEffects(effects);

        player.resetPlayerTime();
        player.resetPlayerWeather();
        player.setVelocity(new Vector(0, 0, 0));
        player.setFallDistance(0f);

        if (items != null) {
            ItemStack[] copy = new ItemStack[items.length];
            for (int i = 0; i < items.length; i++) {
                copy[i] = items[i] == null ? null : items[i].clone();
            }
            player.getInventory().setContents(copy);
        }
    }

    JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("version", VERSION);
        json.addProperty("created", System.currentTimeMillis());
        json.addProperty("gameMode", gameMode.name());

        JsonObject location = new JsonObject();
        if (worldId != null) location.addProperty("worldId", worldId.toString());
        if (worldName != null) location.addProperty("world", worldName);
        location.addProperty("x", x);
        location.addProperty("y", y);
        location.addProperty("z", z);
        location.addProperty("yaw", yaw);
        location.addProperty("pitch", pitch);
        json.add("location", location);

        json.addProperty("foodLevel", foodLevel);
        json.addProperty("saturation", saturation);
        json.addProperty("allowFlight", allowFlight);
        json.addProperty("flying", flying);

        JsonArray effectArray = new JsonArray();
        for (PotionEffect effect : effects) {
            NamespacedKey key = Registry.MOB_EFFECT.getKey(effect.getType());
            if (key == null) {
                continue;
            }

            JsonObject e = new JsonObject();
            e.addProperty("type", key.toString());
            e.addProperty("duration", effect.getDuration());
            e.addProperty("amplifier", effect.getAmplifier());
            e.addProperty("ambient", effect.isAmbient());
            e.addProperty("particles", effect.hasParticles());
            e.addProperty("icon", effect.hasIcon());
            effectArray.add(e);
        }
        json.add("effects", effectArray);

        if (items != null) {
            json.addProperty("items", Base64.getEncoder().encodeToString(ItemStack.serializeItemsAsBytes(items)));
        }

        return json;
    }

    static PlayerBackup fromJson(JsonObject json) {
        JsonObject location = json.getAsJsonObject("location");

        List<PotionEffect> effects = new ArrayList<>();
        JsonElement effectArray = json.get("effects");
        if (effectArray != null && effectArray.isJsonArray()) {
            for (JsonElement element : effectArray.getAsJsonArray()) {
                JsonObject e = element.getAsJsonObject();
                NamespacedKey key = NamespacedKey.fromString(e.get("type").getAsString());
                PotionEffectType type = key == null ? null : Registry.MOB_EFFECT.get(key);
                if (type == null) {
                    continue; // effect no longer exists
                }

                effects.add(new PotionEffect(type, e.get("duration").getAsInt(), e.get("amplifier").getAsInt(),
                        e.get("ambient").getAsBoolean(), e.get("particles").getAsBoolean(), e.get("icon").getAsBoolean()));
            }
        }

        ItemStack[] items = null;
        JsonElement itemData = json.get("items");
        if (itemData != null && !itemData.isJsonNull()) {
            items = ItemStack.deserializeItemsFromBytes(Base64.getDecoder().decode(itemData.getAsString()));
        }

        return new PlayerBackup(GameMode.valueOf(json.get("gameMode").getAsString()),
                location.has("worldId") ? UUID.fromString(location.get("worldId").getAsString()) : null,
                location.has("world") ? location.get("world").getAsString() : null,
                location.get("x").getAsDouble(), location.get("y").getAsDouble(), location.get("z").getAsDouble(),
                location.get("yaw").getAsFloat(), location.get("pitch").getAsFloat(),
                json.get("foodLevel").getAsInt(), json.get("saturation").getAsFloat(),
                json.get("allowFlight").getAsBoolean(), json.get("flying").getAsBoolean(),
                effects, items);
    }

    /**
     * Creates a backup that only holds items, for Infinite Parkour's old inventory backups.
     */
    static PlayerBackup itemsOnly(@NotNull Player player, @NotNull ItemStack[] items) {
        PlayerBackup current = capture(player, false);
        return new PlayerBackup(current.gameMode, current.worldId, current.worldName,
                current.x, current.y, current.z, current.yaw, current.pitch,
                current.foodLevel, current.saturation, current.allowFlight, current.flying,
                current.effects, items);
    }

    /**
     * @return The potion effects, to clear them from the player after capturing.
     */
    public @NotNull Collection<PotionEffect> getEffects() {
        return effects;
    }
}
