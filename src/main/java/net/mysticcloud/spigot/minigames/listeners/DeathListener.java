package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
            Game game = (Game) e.getEntity().getMetadata("game").get(0).value();
            if (!game.getGameState().hasStarted()) e.setCancelled(true);
            if (((Player) e.getEntity()).getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);
                ((Game) e.getEntity().getMetadata("game").get(0).value()).kill((Player) e.getEntity());

            }
        }
    }
}
