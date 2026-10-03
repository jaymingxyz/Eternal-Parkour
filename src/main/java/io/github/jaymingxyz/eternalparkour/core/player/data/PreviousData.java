package io.github.jaymingxyz.eternalparkour.core.player.data;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.config.Config;
import io.github.jaymingxyz.eternalparkour.core.config.Option;
import io.github.jaymingxyz.eternalparkour.core.reward.Reward;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A block-parkour player's state from before they joined, given back when they leave.
 * Also written to disk as a {@link PlayerBackup} (when {@code options.inventory-saving} is on), so it
 * survives a crash.
 */
public class PreviousData {

    private final PlayerBackup backup;

    /**
     * Whether this instance wrote the backup file. Only then is it deleted after restoring.
     */
    private boolean ownsBackupFile = false;

    /**
     * List of all {@link Reward} to execute on leave.
     */
    public List<Reward> onLeave = new ArrayList<>();

    public PreviousData(@NotNull Player player) {
        boolean handleInventory = Config.CONFIG.getBoolean("options.inventory-handling");

        backup = PlayerBackup.capture(player, handleInventory);

        for (PotionEffect effect : backup.getEffects()) {
            player.removePotionEffect(effect.getType());
        }

        if (handleInventory) {
            String command = Config.CONFIG.getString("options.alt-inventory-saving-command");
            if (!command.isEmpty()) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
            }
        }

        if (Config.CONFIG.getBoolean("options.inventory-saving")) {
            PlayerBackups.save(player.getUniqueId(), player.getName(), backup);
            ownsBackupFile = true;
        }
    }

    /**
     * Gives the player back their state.
     *
     * @param player   The player.
     * @param urgent   Whether to do it all right now (the player is quitting or the server is stopping).
     * @param teleport Whether to also send the player back to where they were. False when another plugin
     *                 teleported them out of parkour: they stay where that plugin put them.
     */
    public void apply(Player player, boolean urgent, boolean teleport) {
        if (!teleport) {
            restore(player);
            return;
        }

        Location to = Config.CONFIG.getBoolean("bungeecord.go-back-enabled") ? Option.GO_BACK_LOC : backup.getLocation();
        if (to == null) { // the world they were in no longer exists
            restore(player);
            return;
        }

        if (urgent) {
            player.teleport(to);
            restore(player);
        } else {
            player.teleportAsync(to).thenRun(() -> {
                if (Bukkit.isPrimaryThread()) {
                    restore(player);
                } else {
                    Bukkit.getScheduler().runTask(EternalParkour.getPlugin(), () -> restore(player));
                }
            });
        }
    }

    /**
     * Gives the player back their state, including sending them back.
     *
     * @param player The player.
     * @param urgent Whether to do it all right now.
     */
    public void apply(Player player, boolean urgent) {
        apply(player, urgent, true);
    }

    private void restore(Player player) {
        try {
            backup.applyState(player);
        } catch (Exception ex) {
            // keep the backup file: it's the only copy left
            EternalParkour.logging().stack("Error while giving %s back their state; their backup was kept".formatted(player.getName()), ex);
            return;
        }

        ParkourItems.strip(player);

        if (ownsBackupFile) {
            ownsBackupFile = false;
            PlayerBackups.delete(player.getUniqueId());
        }
    }
}
