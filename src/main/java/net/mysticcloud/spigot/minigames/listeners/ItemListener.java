package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.CTW;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class ItemListener implements Listener {
    public ItemListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getEntity().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                Game.GamePlayer player = game.getGameState().getPlayer(e.getEntity().getUniqueId());
                if (game instanceof CTW) {
                    if (e.getItem().hasMetadata("flag")) {
                        Team team = (Team) e.getItem().getMetadata("flag").get(0).value();
                        e.setCancelled(true);
                        if (!team.equals(player.getTeam()))
                            ((CTW) game).pickupFlag(((Player)e.getEntity()), e.getItem());
                        else if (e.getEntity().hasMetadata("flag")) {
                            ((CTW) game).captureFlag(((Player)e.getEntity()), ((Team)e.getEntity().getMetadata("flag").get(0).value()));
                        }
                    }
                }
            }
        }
    }
}
