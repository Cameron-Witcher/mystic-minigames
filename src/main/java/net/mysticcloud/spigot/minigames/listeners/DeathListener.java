package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.OITQ;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitTask;

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
                if (e.getDamager() instanceof Firework && e.getDamager().hasMetadata("game")) e.setCancelled(true);
                if (e.getEntity() instanceof Item && e.getEntity().hasMetadata("flag")) e.setCancelled(true);

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

                    if (e.getDamager() instanceof Projectile) {
                        if (game instanceof OITQ) {
                            e.setDamage(50);
                            e.getDamager().remove();
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().getWorld().hasMetadata("game")) {
            Game game = (Game) e.getEntity().getWorld().getMetadata("game").get(0).value();
            if (!game.getGameState().hasStarted()) e.setCancelled(true);
            if (((Player) e.getEntity()).getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);
                game.kill((Player) e.getEntity(), e.getCause());

            }
        }
    }
}
