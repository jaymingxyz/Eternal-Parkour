package io.github.jaymingxyz.eternalparkour.core;

import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.player.data.ParkourItems;
import io.github.jaymingxyz.eternalparkour.core.player.data.PlayerBackups;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.core.storage.Storage;
import io.github.jaymingxyz.eternalparkour.core.world.World;
import io.github.jaymingxyz.eternalparkour.core.foundation.event.EventWatcher;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.ParticleData;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.Particles;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Locations;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.ApiStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Internal event handler
 */
@ApiStatus.Internal
public class Events implements EventWatcher {

    // Runs on the async chat thread: only thread-safe lookups (ParkourUser.getUser, Session getters) are used here.
    @EventHandler
    public void chat(AsyncChatEvent event) {
        if (!Boolean.TRUE.equals(Option.OPTIONS_ENABLED.get(ParkourOption.CHAT))) {
            return;
        }

        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);

        if (user == null) {
            return;
        }

        Session session = user.session;

        if (session.isMuted(user)) {
            return;
        }

        String key;
        List<? extends ParkourUser> recipients;
        switch (user.chatType) {
            case LOBBY_ONLY -> {
                key = "settings.chat.formats.lobby";
                recipients = session.getUsers();
            }
            case PLAYERS_ONLY -> {
                key = "settings.chat.formats.players";
                recipients = session.getPlayers();
            }
            default -> {
                return;
            }
        }

        event.setCancelled(true);

        // the message is inserted as a component, so players can't use MiniMessage tags in it
        Component message = event.message();
        recipients.forEach(other -> other.sendChat(key, player.getName(), message));
    }

    // Super Jump in earlier versions raised the maximum air to this and never lowered it. Bukkit saves it
    // with the player; on land air then climbs past what the save format holds (a short) and loads back
    // negative: an empty air bar that doesn't refill on land, and drowning as soon as they're in water.
    private static final int SUPER_JUMP_MAXIMUM_AIR = 100_000_000;
    private static final int DEFAULT_MAXIMUM_AIR = 300;

    private static void repairAir(Player player) {
        if (player.getMaximumAir() == SUPER_JUMP_MAXIMUM_AIR) {
            player.setMaximumAir(DEFAULT_MAXIMUM_AIR);
        }

        // vanilla keeps air between -20 (where drowning damage is dealt) and the maximum
        int air = player.getRemainingAir();
        if (air < -20 || air > player.getMaximumAir()) {
            player.setRemainingAir(player.getMaximumAir());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        repairAir(player);

        // A backup still on disk means the server stopped before this player got their things back.
        // Restore it before anything else can put them into parkour again.
        boolean sentBack = PlayerBackups.restoreOnJoin(player);

        if (Config.CONFIG.getBoolean("bungeecord.enabled")) {
            Modes.DEFAULT.create(player);
            return;
        }

        if (sentBack || !player.getWorld().equals(World.getWorld())) {
            return;
        }

        org.bukkit.World fallback = Bukkit.getWorld(Config.CONFIG.getString("world.fall-back"));

        if (fallback != null) {
            player.teleportAsync(fallback.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN);
            return;
        }

        Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(World.getWorld()))
                .findFirst()
                .ifPresentOrElse(
                        world -> player.teleportAsync(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN),
                        () -> EternalParkour.logging().error("%s joined in the parkour world, but there is no other world to send them to. Set world.fall-back in config.yml.".formatted(player.getName())));
    }

    // Runs async while the player logs in, so joining parkour later never waits on storage.
    @EventHandler(priority = EventPriority.MONITOR)
    public void preLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        Storage.preload(event.getUniqueId());
        PlayerBackups.preload(event.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void forgetSettings(PlayerQuitEvent event) {
        Storage.forget(event.getPlayer().getUniqueId());
        PlayerBackups.forget(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void leave(PlayerQuitEvent event) {
        ParkourUser user = ParkourUser.getUser(event.getPlayer());

        if (user == null) {
            return;
        }

        ParkourUser.unregister(user, true, false, true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void command(PlayerCommandPreprocessEvent event) {
        if (!Config.CONFIG.getBoolean("focus-mode.enabled")) {
            return;
        }

        Player player = event.getPlayer();

        // block and elytra parkour
        if (!ParkourItems.isPlaying(player)) {
            return;
        }

        String label = commandLabel(event.getMessage());
        if (OWN_COMMANDS.contains(label)) {
            return;
        }

        // Whitelist entries are command names (with or without "/"), matched exactly, so "r" allows /r but not /warp.
        boolean whitelisted = Config.CONFIG.getStringList("focus-mode.whitelist").stream()
                .map(Events::commandLabel)
                .anyMatch(label::equals);
        if (whitelisted) {
            return;
        }

        player.sendMessage(Strings.component(Locales.getString(player, "other.no_do")));
        event.setCancelled(true);
    }

    /**
     * Commands of this plugin, always allowed in focus mode (so players can always leave).
     */
    private static final Set<String> OWN_COMMANDS = Set.of(
            "eternalparkour", "ep", "parkour", "witp", "ipp", "ipplus", "iep", "infiniteelytraparkour");

    // "/Plugin:Home foo" -> "home"
    private static String commandLabel(String command) {
        String label = command.strip();
        if (label.startsWith("/")) {
            label = label.substring(1);
        }

        int space = label.indexOf(' ');
        if (space >= 0) {
            label = label.substring(0, space);
        }

        int colon = label.indexOf(':');
        if (colon >= 0) {
            label = label.substring(colon + 1);
        }

        return label.toLowerCase(Locale.ROOT);
    }

    @EventHandler
    public void onPlayerUseBoatOffHand(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // The community menu item is a boat; stop parkour players from placing it. Everyone else is unaffected.
        if (!ParkourUser.isUser(player)) return;

        // 检查是否是副手点击
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;

        // 获取副手物品
        ItemStack offHandItem = player.getInventory().getItem(EquipmentSlot.OFF_HAND);

        // 检查副手物品是否是船
        if (offHandItem != null && offHandItem.getType().toString().endsWith("_BOAT")) {
            // 如果是右键点击方块
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                // 取消事件，防止放置船
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void interactWand(PlayerInteractEvent event) {
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!player.hasPermission("ip.admin") || item.getItemMeta() == null || !isSchematicWand(item) || event.getClickedBlock() == null || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Location location = event.getClickedBlock().getLocation();
        Location[] existingSelection = Command.selections.get(player);

        event.setCancelled(true);

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                send(player, EternalParkour.PREFIX + "Position 1 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{location, null});
                    return;
                }

                Command.selections.put(player, new Location[]{location, existingSelection[1]});

                Particles.box(BoundingBox.of(location, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
            case RIGHT_CLICK_BLOCK -> {
                send(player, EternalParkour.PREFIX + "Position 2 was set to " + Locations.toString(location, true));

                if (existingSelection == null) {
                    Command.selections.put(player, new Location[]{null, location});
                    return;
                }

                Command.selections.put(player, new Location[]{existingSelection[0], location});

                Particles.box(BoundingBox.of(existingSelection[0], location), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void interact(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (pp == null) {
            return;
        }

        boolean type = event.getClickedBlock() != null && (event.getClickedBlock().getType() == Material.DISPENSER || event.getClickedBlock().getType() == Material.DROPPER || event.getClickedBlock().getType() == Material.HOPPER);

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && type && event.getHand() == EquipmentSlot.HAND) {
            event.setCancelled(true);
            player.closeInventory();
            return;
        }

        boolean action = (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND;

        if (!action) {
            return;
        }

        Material held = getHeldItem(player).getType();

        Material play = Locales.getItem(player, "play.item").getMaterial();
        Material community = Locales.getItem(player, "community.item").getMaterial();
        Material settings = Locales.getItem(player, "settings.item").getMaterial();
        Material lobby = Locales.getItem(player, "lobby.item").getMaterial();
        Material quit = Locales.getItem(player, "other.quit").getMaterial();

        event.setCancelled(true);

        if (held == play) {
            Menus.PLAY.open(player);
        } else if (held == community) {
            Menus.COMMUNITY.open(player);
        } else if (held == settings) {
            Menus.SETTINGS.open(player);
        } else if (held == lobby) {
            Menus.LOBBY.open(player);
        } else if (held == quit) {
            ParkourUser.leave(player);
        } else {
            if (!Config.CONFIG.getBoolean("options.disable-inventory-blocks")) {
                event.setCancelled(false);
            }
        }
    }

    // Paper 26: ItemMeta#getDisplayName() is deprecated (returns a legacy String). Use the
    // Component-returning displayName() and convert to plain text for substring matching.
    private static boolean isSchematicWand(ItemStack item) {
        Component name = item.getItemMeta() == null ? null : item.getItemMeta().displayName();
        if (name == null) return false;
        return PlainTextComponentSerializer.plainText().serialize(name).contains("Schematic Wand");
    }

    private ItemStack getHeldItem(Player player) {
        PlayerInventory inventory = player.getInventory();
        return inventory.getItemInMainHand().getType() == Material.AIR ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }

    @EventHandler
    public void switchWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        ParkourUser user = ParkourUser.getUser(player);
        org.bukkit.World parkour = World.getWorld();

        boolean isAdmin = Config.CONFIG.getBoolean("permissions.enabled") ? ParkourOption.ADMIN.mayPerform(player) : player.isOp();

        if (player.getWorld() == parkour && user == null && !isAdmin && player.getTicksLived() > 20) {
            Bukkit.getWorlds().stream()
                    .filter(world -> !world.equals(parkour))
                    .findAny()
                    .ifPresent(world -> player.teleportAsync(world.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN));
            return;
        }

        if (event.getFrom() == parkour && user != null && Duration.between(user.joined, Instant.now()).toMillis() > 100) {
            // Another plugin (e.g. /home, /spawn, /tpa) teleported them out: give their things back,
            // but leave them where that plugin put them.
            ParkourUser.unregister(user, true, false, false, false);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        handleRestriction(event.getPlayer(), event);
    }

    @EventHandler
    public void damage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void inventory(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || event.getInventory().getType() == InventoryType.CRAFTING) {
            return;
        }

        handleRestriction(player, event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void spectate(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
            return;
        }

        handleRestriction(event.getPlayer(), event);
    }

//    @EventHandler(priority = EventPriority.HIGHEST)
//    public void mount(EntityMountEvent event) {
//        if (!(event.getEntity() instanceof Player player)) {
//            return;
//        }
//
//        handleRestriction(player, event);
//    }

    private void handleRestriction(Player player, Cancellable event) {
        if (!ParkourUser.isUser(player)) {
            return;
        }

        event.setCancelled(true);
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}