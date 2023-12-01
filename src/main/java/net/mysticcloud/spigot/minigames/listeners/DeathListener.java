package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class DeathListener implements Listener {
    public DeathListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().hasMetadata("game")) {
            e.getEntity().setMetadata("last_damager", new FixedMetadataValue(Utils.getPlugin(), e.getDamager().getUniqueId()));
            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                e.getEntity().removeMetadata("last_damager", Utils.getPlugin());
            }, 5 * 20);
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().hasMetadata("game")) {
            Bukkit.broadcastMessage("Damage: " + e.getDamage());
            Bukkit.broadcastMessage("Final Damage: " + e.getFinalDamage());
            Bukkit.broadcastMessage("Health: " + ((Player) e.getEntity()).getHealth());
//            if(((Player) e.getEntity()).getHealthScale() - e.getFinalDamage())
        }
    }
}
