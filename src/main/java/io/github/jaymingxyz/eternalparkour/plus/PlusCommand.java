package io.github.jaymingxyz.eternalparkour.plus;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.foundation.command.ViCommand;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.ParticleData;
import io.github.jaymingxyz.eternalparkour.core.foundation.particle.Particles;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Locations;
import io.github.jaymingxyz.eternalparkour.core.foundation.util.Strings;
import io.github.jaymingxyz.eternalparkour.core.menu.ParkourOption;
import io.github.jaymingxyz.eternalparkour.plus.menu.ActiveMenu;
import io.github.jaymingxyz.eternalparkour.plus.menu.InviteMenu;
import io.github.jaymingxyz.eternalparkour.plus.menu.MultiplayerMenu;
import io.github.jaymingxyz.eternalparkour.plus.mode.lobby.Lobby;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PlusCommand extends ViCommand {

    public static final Map<Player, Location> pos1s = new WeakHashMap<>();
    public static final Map<Player, Location> pos2s = new WeakHashMap<>();

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }

        if (args.length == 0) {
            help(sender);
            return true;
        } else if (args.length == 1) {
            switch (args[0].toLowerCase()) {
                case "lobbies", "lobby" -> {
                    if (sender instanceof Player && PlusOption.ACTIVE.mayPerform(player)) {
                        ActiveMenu.open(player, ActiveMenu.MenuSort.LEAST_OPEN_FIRST);
                    }
                }
                case "create", "multiplayer" -> {
                    if (sender instanceof Player && PlusOption.MULTIPLAYER.mayPerform(player)) {
                        MultiplayerMenu.open(player);
                    }
                }
                case "invite" -> {
                    if (sender instanceof Player && PlusOption.INVITE.mayPerform(player)) {
                        InviteMenu.open(player);
                    }
                }
                // every module reloads with /ep reload; kept so the old command still works
                case "reload" -> Bukkit.dispatchCommand(sender, "eternalparkour:eternalparkour reload");
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("lobbygm") && player != null && sender.hasPermission(ParkourOption.ADMIN.permission)) {

                Location location = player.getLocation();
                @Nullable var pos1 = pos1s.get(player);
                @Nullable var pos2 = pos2s.get(player);

                switch (args[1]) {
                    case "pos1" -> {
                        send(player, "%sPosition 1 was set to %s".formatted(IPP.PREFIX, Locations.toString(location, true)));

                        pos1s.put(player, location);

                        if (pos1 == null || pos2 == null) return true;

                        Particles.box(BoundingBox.of(location, pos2), player.getWorld(),
                                new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "pos2" -> {
                        send(player, "%sPosition 2 was set to %s".formatted(IPP.PREFIX, Locations.toString(location, true)));

                        pos2s.put(player, location);

                        if (pos1 == null || pos2 == null) return true;

                        Particles.box(BoundingBox.of(pos1, location), player.getWorld(),
                                new ParticleData<>(Particle.END_ROD, null, 2), player, 0.2);
                    }
                    case "save" -> {
                        if (pos1 == null || pos2 == null) {
                            send(player, "%sYour lobby area isn't complete yet. Be sure to set the first and second position!".formatted(IPP.PREFIX));
                            return true;
                        }

                        BoundingBox bb = BoundingBox.of(pos1, pos2);

                        if (bb.getWidthX() < Lobby.MINIMUM_SIZE ||
                                bb.getHeight() < Lobby.MINIMUM_SIZE ||
                                bb.getWidthZ() < Lobby.MINIMUM_SIZE) {
                            send(player, "%sYou haven't made the area big enough! It needs to be at least <bold>%d</bold> blocks in all directions.".formatted(IPP.PREFIX, Lobby.MINIMUM_SIZE));
                            return true;
                        }

                        send(player, "%sYour lobby area selection is being saved.".formatted(IPP.PREFIX));
                        Lobby.save(player.getWorld(), bb);
                    }
                }
            }
        }
        return true;
    }

    private void help(CommandSender sender) {
        send(sender, "");
        send(sender, EternalParkour.HEADER);
        send(sender, "");
        send(sender, "<gray>/ipp create <dark_gray>- Create a multiplayer lobby");
        send(sender, "<gray>/ipp lobbies <dark_gray>- View the open lobbies");
        send(sender, "<gray>/ipp invite <dark_gray>- Invite players to your lobby");
        if (sender.hasPermission(ParkourOption.ADMIN.permission)) {
            send(sender, "<gray>/ipp lobbygm <pos1/pos2/save> <dark_gray>- Set up the lobby mode area");
        }
        send(sender, "");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1 -> {
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
                    completions.add("lobbygm");
                }
                return completions(args[0], completions);
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("lobbygm") && sender.hasPermission(ParkourOption.ADMIN.permission)) {
                    completions.addAll(List.of("pos1", "pos2", "save"));
                }
                return completions(args[1], completions);
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(Strings.colour(message));
    }
}