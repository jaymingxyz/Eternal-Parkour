package io.github.jaymingxyz.eternalparkour.core;

import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.config.Locales;
import io.github.jaymingxyz.eternalparkour.core.leaderboard.Leaderboard;
import io.github.jaymingxyz.eternalparkour.core.menu.Menus;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.mode.MultiMode;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourUser;
import io.github.jaymingxyz.eternalparkour.core.player.data.ParkourItems;
import io.github.jaymingxyz.eternalparkour.core.player.data.PlayerBackups;
import io.github.jaymingxyz.eternalparkour.core.session.Session;
import io.github.jaymingxyz.eternalparkour.plus.PlusOption;
import io.github.jaymingxyz.eternalparkour.core.foundation.command.ViCommand;
import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.ParticleData;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.Particles;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematic;
import io.github.jaymingxyz.eternalparkour.core.foundation.schematic.Schematics;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Locations;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import org.bukkit.*;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("deprecation")
public class Command extends ViCommand {

    // WeakHashMap so entries for offline players are GC'd automatically.
    // Using a hard HashMap<Player, ...> kept CraftPlayer (and its inventory/entity tracker)
    // alive forever for any admin who used /ep schematic pos1/pos2 or the wand.
    public static final Map<Player, Location[]> selections = new WeakHashMap<>();

    private static final ItemStack WAND = new Item(Material.GOLDEN_AXE, "<red><bold>Schematic Wand")
            .lore("<gray>Left click: first position", "<gray>Right click: second position")
            .build();

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        switch (args.length) {
            case 0 -> handle0Args(sender, player);
            case 1 -> handle1Args(args[0], sender, player);
            case 2 -> handle2Args(args[0], args[1], sender, player);
            case 3 -> handle3Args(args[0], args[1], args[2], sender, player);
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
                if (ParkourOption.JOIN.mayPerform(sender)) {
                    completions.add("join");
                    completions.add("leave");
                }
                if (ParkourOption.MAIN.mayPerform(sender)) {
                    completions.add("menu");
                }
                if (ParkourOption.PLAY.mayPerform(sender)) {
                    completions.add("play");
                }
                if (ParkourOption.LEADERBOARDS.mayPerform(sender)) {
                    completions.add("leaderboard");
                }
                if (PlusOption.MULTIPLAYER.mayPerform(sender)) {
                    completions.add("create");
                }
                if (PlusOption.ACTIVE.mayPerform(sender)) {
                    completions.add("lobbies");
                }
                if (PlusOption.INVITE.mayPerform(sender)) {
                    completions.add("invite");
                }
                if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("schematic");
                    completions.add("reload");
                    completions.add("forcejoin");
                    completions.add("forceleave");
                    completions.add("reset");
                    completions.add("recoverinventory");
                }
                return completions(args[0], completions);
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("reset") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("join") && sender.hasPermission(ParkourOption.JOIN.permission)) {
                    for (ParkourPlayer pp : ParkourPlayer.getPlayers()) {
                        completions.add(pp.getName());
                    }
                } else if (args[0].equalsIgnoreCase("schematic") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.addAll(Arrays.asList("wand", "pos1", "pos2", "save", "paste"));
                } else if (args[0].equalsIgnoreCase("forcejoin") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("nearest");
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("forceleave") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.add("everyone");

                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                } else if (args[0].equalsIgnoreCase("recoverinventory") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    for (Player pl : Bukkit.getOnlinePlayers()) {
                        completions.add(pl.getName());
                    }
                }
                return completions(args[1], completions);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    private void handle0Args(@NotNull CommandSender sender, @Nullable Player player) {
        if (player != null && ParkourOption.MAIN.mayPerform(player)) {
            Menus.MAIN.open(player);
            return;
        }
        sendHelpMessages(sender);
    }

    private void sendHelpMessages(CommandSender sender) {
        send(sender, "");
        send(sender, EternalParkour.HEADER);
        send(sender, "");
        send(sender, "<gray>/ep <dark_gray>- Main command");
        if (sender.hasPermission(ParkourOption.JOIN.permission)) {
            send(sender, "<gray>/ep join [mode/player] <dark_gray>- Join the default mode or specify one.");
            send(sender, "<gray>/ep leave <dark_gray>- Leave the game on this server");
        }
        if (sender.hasPermission(ParkourOption.MAIN.permission)) {
            send(sender, "<gray>/ep menu <dark_gray>- Open the menu");
        }
        if (sender.hasPermission(ParkourOption.PLAY.permission)) {
            send(sender, "<gray>/ep play <dark_gray>- Mode selection menu");
        }
        if (sender.hasPermission(ParkourOption.LEADERBOARDS.permission)) {
            send(sender, "<gray>/ep leaderboard [type]<dark_gray>- Open the leaderboard of a mode");
        }
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/ep schematic <dark_gray>- Create a schematic");
            send(sender, "<gray>/ep reload <dark_gray>- Reloads all config and locale files, including multiplayer and elytra");
            send(sender, "<gray>/ep reset <everyone/player> <dark_gray>- Resets all high scores. <red>This can't be recovered!");
            send(sender, "<gray>/ep forcejoin <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to join");
            send(sender, "<gray>/ep forceleave <everyone/nearest/player> <dark_gray>- Forces a specific player, the nearest or everyone to leave");
            send(sender, "<gray>/ep recoverinventory <player> [confirm] <dark_gray>- Restore a player's inventory backup after a crash. <red>Replaces their current inventory.");
        }
        send(sender, "");
    }

    private void handle1Args(@NotNull String arg, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg.toLowerCase()) {
            case "help" -> sendHelpMessages(sender);
            case "reload" -> {
                if (!cooldown(sender, "reload", 2500)) {
                    return;
                }
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                if (EternalParkour.reloadConfigs()) {
                    send(sender, "%sReloaded all config files.".formatted(EternalParkour.PREFIX));
                } else {
                    send(sender, "%s<red>Some config files couldn't be reloaded. See the console for details.".formatted(EternalParkour.PREFIX));
                }
            }
        }

        if (player == null) {
            return;
        }

        switch (arg.toLowerCase()) {
            // multiplayer menus, handled by /ipp
            case "create", "multiplayer", "lobbies", "lobby", "invite" -> player.performCommand("ipp " + arg.toLowerCase());
            case "join" -> {
                if (!cooldown(sender, "join", 2500)) {
                    return;
                }

                if (!ParkourOption.JOIN.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                if (user != null) {
                    return;
                }

                Modes.DEFAULT.create(player);
            }
            case "play" -> {
                if (ParkourOption.PLAY.mayPerform(player)) {
                    Menus.PLAY.open(player);
                }
            }
            case "leave" -> {
                if (!cooldown(sender, "leave", 2500)) {
                    return;
                }
                ParkourUser.leave(player);
            }
            case "menu", "main" -> {
                if (!ParkourOption.MAIN.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Menus.MAIN.open(player);
            }
            case "leaderboard" -> {
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Menus.LEADERBOARDS.open(player);
            }
            case "schematic" -> {
                if (!player.hasPermission(ParkourOption.ADMIN.permission)) { // default players shouldn't have access even if perms are disabled
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                send(player, "");
                send(player, "<red>/ep schematic wand <dark_gray>- <gray>Get the schematic wand");
                send(player, "<red>/ep schematic pos1 <dark_gray>- <gray>Set the first position of your selection");
                send(player, "<red>/ep schematic pos2 <dark_gray>- <gray>Set the second position of your selection");
                send(player, "<red>/ep schematic save <dark_gray>- <gray>Save your selection to a schematic file");
                send(player, "<red>/ep schematic paste <file> <dark_gray>- <gray>Paste a schematic file");
                send(player, "");
                send(player, "<dark_gray><underlined>Have any questions or need help? Join the Discord.");
            }
        }
    }

    private void handle2Args(@NotNull String arg1, @NotNull String arg2, @NotNull CommandSender sender, @Nullable Player player) {
        switch (arg1.toLowerCase()) {
            case "forcejoin" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    Bukkit.getOnlinePlayers().forEach(other -> Modes.DEFAULT.create(other));
                    send(sender, EternalParkour.PREFIX + "Successfully force joined everyone!");
                    return;
                }

                if (arg2.equalsIgnoreCase("nearest")) {
                    Player closest = null;
                    double distance = Double.MAX_VALUE;

                    // if player is found get location from player
                    // if no player is found, get location from command block
                    // if no command block is found, return null
                    Location from = sender instanceof Player ? ((Player) sender).getLocation() : (sender instanceof BlockCommandSender ? ((BlockCommandSender) sender).getBlock().getLocation() : null);

                    if (from == null || from.getWorld() == null) {
                        return;
                    }

                    // get the closest player
                    for (Player p : from.getWorld().getPlayers()) {
                        if (p == sender) {
                            continue; // a player running the command means the nearest other player
                        }

                        double d = p.getLocation().distance(from);

                        if (d < distance) {
                            distance = d;
                            closest = p;
                        }
                    }

                    // no closest player found
                    if (closest == null) {
                        return;
                    }

                    send(sender, EternalParkour.PREFIX + "Successfully force joined " + closest.getName() + "!");
                    Modes.DEFAULT.create(closest);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, EternalParkour.PREFIX + "That player isn't online!");
                    return;
                }

                Modes.DEFAULT.create(other);
            }
            case "forceleave" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    ParkourPlayer.getPlayers().forEach(ParkourUser::leave);
                    send(sender, EternalParkour.PREFIX + "Successfully force kicked everyone!");
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);
                if (other == null) {
                    send(sender, EternalParkour.PREFIX + "That player isn't online!");
                    return;
                }

                ParkourUser user = ParkourUser.getUser(other);
                if (user == null) {
                    send(sender, EternalParkour.PREFIX + "That player isn't currently playing!");
                    return;
                }

                ParkourUser.leave(user);
            }
            case "reset" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission) || !cooldown(sender, "reset", 2500)) {
                    return;
                }

                if (arg2.equalsIgnoreCase("everyone")) {
                    for (Mode mode : Registry.getModes()) {
                        Leaderboard leaderboard = mode.getLeaderboard();

                        if (leaderboard == null) {
                            continue;
                        }

                        leaderboard.resetAll();
                        leaderboard.write(true);
                    }

                    send(sender, EternalParkour.PREFIX + "Successfully reset all high scores in memory and the files.");
                    return;
                }
                String name = null;
                UUID uuid = null;

                // Check online players
                Player online = Bukkit.getPlayerExact(arg2);
                if (online != null) {
                    name = online.getName();
                    uuid = online.getUniqueId();
                }

                // Check uuid
                if (uuid == null && arg2.contains("-")) {
                    try {
                        uuid = UUID.fromString(arg2);
                        name = arg2;
                    } catch (IllegalArgumentException ex) {
                        send(sender, EternalParkour.PREFIX + "That isn't a valid UUID.");
                        return;
                    }
                }

                // Check offline players who have joined before (never looked up online: that would block the server)
                if (uuid == null) {
                    OfflinePlayer offline = Bukkit.getOfflinePlayerIfCached(arg2);
                    if (offline == null) {
                        send(sender, EternalParkour.PREFIX + "Unknown player %s. Use their UUID instead.".formatted(arg2));
                        return;
                    }
                    name = offline.getName();
                    uuid = offline.getUniqueId();
                }

                UUID finalUuid = uuid;
                String finalName = name;

                for (Mode mode : Registry.getModes()) {
                    Leaderboard leaderboard = mode.getLeaderboard();

                    if (leaderboard == null) {
                        continue;
                    }

                    leaderboard.remove(finalUuid);
                    leaderboard.write(true);
                }

                send(sender, EternalParkour.PREFIX + "Successfully reset the high score of " + finalName + " in memory and the files.");
            }
            case "recoverinventory" -> recoverInventory(sender, arg2, false);
        }

        if (player == null) {
            return;
        }

        switch (arg1.toLowerCase()) {
            case "join" -> {
                if (!cooldown(sender, "join", 2500) || !ParkourOption.JOIN.mayPerform(player)) {
                    return;
                }

                Mode mode = Registry.getMode(arg2);

                if (mode != null) {
                    mode.create(player);
                    return;
                }

                Player other = Bukkit.getPlayer(arg2);

                if (other == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(EternalParkour.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourPlayer parkourPlayer = ParkourPlayer.getPlayer(other);

                if (parkourPlayer == null) {
                    send(sender, "%sUnknown player! Try typing the name again.".formatted(EternalParkour.PREFIX)); // could not find, so go to default
                    return;
                }

                ParkourUser user = ParkourUser.getUser(player);
                Session session = parkourPlayer.session;
                if (user != null && user.session == session) { // already in same session
                    return;
                }

                if (session.isAcceptingPlayers()) {
                    ((MultiMode) session.generator.getMode()).join(player, session);
                } else {
                    Modes.SPECTATOR.create(player, session);
                }
            }
            case "leaderboard" -> {
                if (!ParkourOption.LEADERBOARDS.mayPerform(player)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Mode mode = Registry.getMode(arg2.toLowerCase());

                // if found gamemode is null, return to default
                if (mode == null) {
                    Menus.LEADERBOARDS.open(player);
                } else {
                    Menus.SINGLE_LEADERBOARD.open(player, mode, Leaderboard.Sort.SCORE);
                }
            }
            case "schematic" -> {
                if (!sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    send(sender, Locales.getString(player, "other.no_do"));
                    return;
                }

                Location playerLocation = player.getLocation();
                Location[] existingSelection = selections.get(player);

                switch (arg2.toLowerCase()) {
                    case "wand" -> {
                        player.getInventory().addItem(WAND);

                        send(player, "<dark_gray>----------- <dark_red><bold>Schematics <reset><dark_gray>-----------");
                        send(player, "<gray><red>Left click<gray> -> set first position | <red>Right click<gray> -> set second position");
                        send(player, "<gray>If you can't place a block and need to set a position mid-air, use <dark_gray>/ep schematic pos1/pos2 <gray>instead.");
                    }
                    case "pos1" -> {
                        send(player, "%sPosition 1 was set to %s".formatted(EternalParkour.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{playerLocation, null});
                            return;
                        }

                        selections.put(player, new Location[]{playerLocation, existingSelection[1]});

                        Particles.box(BoundingBox.of(playerLocation, existingSelection[1]), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "pos2" -> {
                        send(player, "%sPosition 2 was set to %s".formatted(EternalParkour.PREFIX, Locations.toString(playerLocation, true)));

                        if (existingSelection == null) {
                            selections.put(player, new Location[]{null, playerLocation});
                            return;
                        }

                        selections.put(player, new Location[]{existingSelection[0], playerLocation});

                        Particles.box(BoundingBox.of(existingSelection[0], playerLocation), player.getWorld(), new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "save" -> {
                        if (!cooldown(sender, "IP save schematic", 2500)) {
                            return;
                        }

                        if (existingSelection == null || existingSelection[0] == null || existingSelection[1] == null) {
                            send(player, "<dark_red><bold>Schematics <reset><gray>Your schematic isn't complete yet. Make sure you've set the first and second position.");
                            return;
                        }

                        String code = UUID.randomUUID().toString().split("-")[0];

                        if (!Schematic.save(EternalParkour.getInFolder("schematics/parkour-%s".formatted(code)), existingSelection[0], existingSelection[1], EternalParkour.getPlugin())) {
                            send(player, "<dark_red><bold>Schematics <reset><gray>Your selection only contains air, so nothing was saved.");
                            return;
                        }

                        send(player, ("<dark_red><bold>Schematics <reset><gray>Your schematic is being saved. It will use code <red>'%s'<gray>. " + "You can change the code to whatever you like. " + "Don't forget to add this schematic to <dark_gray>schematics.yml<gray>.").formatted(code));
                    }
                }
            }
        }
    }

    /**
     * Restores a player's inventory backup. Without confirmation, only reports what would happen.
     */
    private void recoverInventory(@NotNull CommandSender sender, @NotNull String name, boolean confirmed) {
        if (!sender.hasPermission(ParkourOption.ADMIN.permission) || !cooldown(sender, "recoverinventory", 1000)) {
            return;
        }

        Player other = Bukkit.getPlayerExact(name);
        if (other == null) {
            send(sender, EternalParkour.PREFIX + "That player isn't online!");
            return;
        }

        if (ParkourItems.isPlaying(other)) {
            send(sender, EternalParkour.PREFIX + "%s is currently playing. Their things are given back when they leave, so let them leave first.".formatted(other.getName()));
            return;
        }

        PlayerBackups.loadForRecovery(other, backup -> {
            if (backup == null) {
                send(sender, EternalParkour.PREFIX + "%s has no backup (or it couldn't be read; check the console).".formatted(other.getName()));
                return;
            }

            if (!confirmed) {
                send(sender, "%sFound a backup of %s with %d inventory stacks.".formatted(EternalParkour.PREFIX, other.getName(), backup.countItems()));
                send(sender, "%s<red>Restoring it replaces their current inventory and gamemode.<gray> Run <white>/ep recoverinventory %s confirm<gray> to restore it."
                        .formatted(EternalParkour.PREFIX, other.getName()));
                return;
            }

            if (!other.isOnline() || ParkourItems.isPlaying(other)) {
                send(sender, EternalParkour.PREFIX + "%s left or joined parkour; nothing was restored.".formatted(other.getName()));
                return;
            }

            backup.applyState(other);
            ParkourItems.strip(other);
            PlayerBackups.deleteAfterRecovery(other.getUniqueId());
            send(sender, "%sRestored %s's backup (%d inventory stacks) and deleted it.".formatted(EternalParkour.PREFIX, other.getName(), backup.countItems()));
        });
    }

    private void handle3Args(@NotNull String arg1, @NotNull String arg2, @NotNull String arg3, @NotNull CommandSender sender, @Nullable Player player) {
        if (arg1.equalsIgnoreCase("recoverinventory") && arg3.equalsIgnoreCase("confirm")) {
            recoverInventory(sender, arg2, true);
            return;
        }

        if (player == null) {
            return;
        }

        if (arg1.equalsIgnoreCase("schematic")) {
            if (!arg2.equalsIgnoreCase("paste")) {
                return;
            }

            if (!player.hasPermission(ParkourOption.ADMIN.permission)) {
                return;
            }

            Schematic schematic = Schematics.getSchematic(EternalParkour.getPlugin(), arg3);
            if (schematic == null) {
                send(sender, "%sCouldn't find %s".formatted(EternalParkour.PREFIX, arg3));
                return;
            }

            schematic.paste(player.getLocation());
            send(sender, "%sPasted schematic %s".formatted(EternalParkour.PREFIX, arg3));
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}