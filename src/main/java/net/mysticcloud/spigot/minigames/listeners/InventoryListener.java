package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.metadata.MetadataValue;

public class InventoryListener implements Listener {
    public InventoryListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked().getWorld().hasMetadata("game")) {
            for (MetadataValue data : e.getWhoClicked().getWorld().getMetadata("game")) {
                Game game = (Game) data.value();
                    if (e.getSlotType().equals(InventoryType.SlotType.ARMOR)) e.setCancelled(true);

            }
        }
    }
}
