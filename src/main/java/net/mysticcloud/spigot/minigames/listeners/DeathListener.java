package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.HotPotato;
import net.mysticcloud.spigot.minigames.utils.games.OITQ;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.UUID;

public class DeathListener implements Listener {
    public DeathListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity().getWorld().hasMetadata("game")) {
            for (MetadataValue data : e.getEntity().getWorld().getMetadata("game")) {
                Game game = (Game) data.value();
                if (game instanceof OITQ) {
                    e.getEntity().remove();
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent e) {
        if (e.getEntity().getWorld().hasMetadata("game")) {
            for (MetadataValue data : e.getEntity().getWorld().getMetadata("game")) {
                Game game = (Game) data.value();
                if (e.getDamager() instanceof Firework && e.getDamager().hasMetadata("game")) {
                    e.setCancelled(true);
                    return;
                }
                if (e.getEntity() instanceof Item && e.getEntity().hasMetadata("flag")) {
                    e.setCancelled(true);
                    e.getEntity().setVelocity(new Vector(0, 0, 0));
                    return;
                }

                if (e.getEntity() instanceof Player) {
                    if (e.getEntity().hasMetadata("last_damager")) {
                        e.getEntity().removeMetadata("last_damager", Utils.getPlugin());
                        Bukkit.getScheduler().cancelTask(((BukkitTask) e.getEntity().getMetadata("last_damager_timer").get(0).value()).getTaskId());
                        e.getEntity().removeMetadata("last_damager_timer", Utils.getPlugin());
                    }
                    e.getEntity().setMetadata("last_damager", new FixedMetadataValue(Utils.getPlugin(), (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof LivingEntity ? ((LivingEntity) ((Projectile) e.getDamager()).getShooter()).getUniqueId() : e.getDamager().getUniqueId())));
                    e.getEntity().setMetadata("last_damager_timer", new FixedMetadataValue(Utils.getPlugin(), Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                        e.getEntity().removeMetadata("last_damager", Utils.getPlugin());
                        e.getEntity().removeMetadata("last_damager_timer", Utils.getPlugin());
                    }, 7 * 20)));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageEvent e) {
        Bukkit.broadcastMessage("1");
        if (e.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM)) {
            e.setCancelled(false);
            return;
        }
        Bukkit.broadcastMessage("2");
        if (e.getEntity() instanceof Player && e.getEntity().getWorld().hasMetadata("game")) {
            Bukkit.broadcastMessage("3");
            Game game = (Game) e.getEntity().getWorld().getMetadata("game").get(0).value();
            Bukkit.broadcastMessage("4");
            if (!game.getGameState().hasStarted()) e.setCancelled(true);
            Bukkit.broadcastMessage("5");
            game.processDamage((Player)e.getEntity(), e.getDamage(), e.getCause());
            Bukkit.broadcastMessage("17");
            e.setCancelled(true);
            Bukkit.broadcastMessage("18");


        }

    }
}
