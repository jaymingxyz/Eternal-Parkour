package io.github.jaymingxyz.eternalparkour.core.reward;

import io.github.jaymingxyz.eternalparkour.core.EternalParkour;
import io.github.jaymingxyz.eternalparkour.core.api.Registry;
import io.github.jaymingxyz.eternalparkour.core.hook.VaultHook;
import io.github.jaymingxyz.eternalparkour.core.mode.Mode;
import io.github.jaymingxyz.eternalparkour.core.mode.Modes;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * A class for handling reward commands, etc.
 */
// todo add cross-server support
// todo mode-specific way of saving one-time-rewards
public record Reward(@NotNull String string) {

    /**
     * Parses and executes this reward
     *
     * @param player The player to which to give this reward to
     */
    public void execute(@NotNull ParkourPlayer player, @NotNull Mode mode) {
        if (string.isEmpty()) {
            return;
        }

        String string = this.string;

        Mode rewardMode = Registry.getModes().stream()
                .filter(m -> this.string.contains("%s:".formatted(m.getName().toLowerCase())))
                .findFirst()
                .orElse(Modes.DEFAULT);

        if (mode != rewardMode) {
            return;
        }

        string = string.replaceFirst("%s:".formatted(rewardMode.getName().toLowerCase()), "");

        // check for placeholders
        if (string.toLowerCase().contains("%player%")) {
            string = string.replaceAll("%player%", player.getName());
        }

        // check for extra data
        if (string.toLowerCase().contains("leave:")) { // leave:
            string = string.replaceFirst("leave:", "");
            player.previousData.onLeave.add(new Reward(string));
            return;
        }

        // check for things to perform
        if (string.toLowerCase().contains("send:")) {
            string = string.replaceFirst("send:", "");

            player.send(string);
        } else if (string.toLowerCase().contains("vault:")) {
            string = string.replaceFirst("vault:", "");

            try {
                VaultHook.deposit(player.player, Double.parseDouble(string));
            } catch (NumberFormatException ex) {
                EternalParkour.logging().stack("Error while trying to process Vault reward", "check your rewards file for incorrect numbers", ex);
            }
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), string);
        }
    }
}