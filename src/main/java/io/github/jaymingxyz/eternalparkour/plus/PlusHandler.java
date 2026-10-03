package io.github.jaymingxyz.eternalparkour.plus;

import io.github.jaymingxyz.eternalparkour.core.foundation.inventory.item.Item;
import io.github.jaymingxyz.eternalparkour.core.player.ParkourPlayer;
import io.github.jaymingxyz.eternalparkour.plus.config.PlusLocales;
import io.github.jaymingxyz.eternalparkour.plus.generator.multi.DuelsGenerator;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlusHandler implements Listener {

    @EventHandler
    public void click(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ParkourPlayer pp = ParkourPlayer.getPlayer(player);

        if (event.getHand() != EquipmentSlot.HAND || pp == null || event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        Item start = PlusLocales.getItem(pp.locale, "play.multi.duels.start");

        if (item == null || item.getType() != start.getMaterial() || !(pp.session.generator instanceof DuelsGenerator generator)) {
            return;
        }

        if (generator.getPlayers().size() > 1) {
            generator.initCountdown();
            pp.player.getInventory().remove(start.getMaterial());
        } else {
            pp.send(PlusLocales.getString(player, "play.multi.duels.duel_self", false));
        }
    }
}