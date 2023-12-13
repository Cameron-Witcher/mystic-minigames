package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EntityListener implements Listener {
    public EntityListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent e) {
        if(e.getLocation().getWorld().hasMetadata("game") && e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.NATURAL)){
            e.setCancelled(true);
            return;
        }
    }
}
