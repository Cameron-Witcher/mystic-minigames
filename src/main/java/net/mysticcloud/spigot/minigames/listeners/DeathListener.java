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
        Bukkit.broadcastMessage(e.getEntity().getName() + "1-1");
        Bukkit.broadcastMessage(e.getEntity().getName() + "1-2");
        Bukkit.broadcastMessage(e.getEntity().getName() + "1-3");
        if (e.getEntity().hasMetadata("shop") || e.getEntity().hasMetadata("do_not_damage")) {
            e.setCancelled(true);
            return;
        }
        if (e.getEntity().getWorld().hasMetadata("game")) {
            Bukkit.broadcastMessage(e.getEntity().getName() + "1-4");
            for (MetadataValue data : e.getEntity().getWorld().getMetadata("game")) {
                Game game = (Game) data.value();

                if (e.getEntity() instanceof Item && e.getEntity().hasMetadata("flag")) {
                    e.setCancelled(true);
                    e.getEntity().setVelocity(new Vector(0, 0, 0));
                    return;
                }
                Bukkit.broadcastMessage(e.getEntity().getName() + "1-6");

                if (e.getEntity() instanceof Player) {
                    Bukkit.broadcastMessage(e.getEntity().getName() + "1-7");
                    Player player = (Player) e.getEntity();
                    if (e.getEntity().hasMetadata("last_damager")) {
                        e.getEntity().removeMetadata("last_damager", Utils.getPlugin());
                        Bukkit.getScheduler().cancelTask(((BukkitTask) e.getEntity().getMetadata("last_damager_timer").get(0).value()).getTaskId());
                        e.getEntity().removeMetadata("last_damager_timer", Utils.getPlugin());
                    }
                    e.getEntity().setMetadata("last_damager", new FixedMetadataValue(Utils.getPlugin(), (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof LivingEntity ? ((LivingEntity) ((Projectile) e.getDamager()).getShooter()) : e.getDamager())));
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
        Bukkit.broadcastMessage(e.getEntity().getName() + "3-1");
        if (e.getEntity().hasMetadata("do_damage")) {
            Bukkit.broadcastMessage(e.getEntity().getName() + "3-2");
            e.setCancelled(false);
            e.getEntity().removeMetadata("do_damage", Utils.getPlugin());
            return;
        }
        Bukkit.broadcastMessage(e.getEntity().getName() + "3-3");
        if (e.getEntity() instanceof Player && e.getEntity().getWorld().hasMetadata("game")) {
            Bukkit.broadcastMessage(e.getEntity().getName() + "3-4");
            Game game = (Game) e.getEntity().getWorld().getMetadata("game").get(0).value();
            if (!game.getGameState().hasStarted()) {
                e.setCancelled(true);
                return;
            }
            Bukkit.broadcastMessage(e.getEntity().getName() + "3-5");
            game.getGameState().processDamage((Player) e.getEntity(), e.getDamage(), e.getCause());
            Bukkit.broadcastMessage(e.getEntity().getName() + "3-6");
            e.setCancelled(true);


        }

    }
}
