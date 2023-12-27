package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.games.Dodgebolt;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
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
                if (game instanceof Dodgebolt) {
                    if (e.getItem().hasMetadata("flag")) {
                        e.setCancelled(true);
                        Team flag = (Team) e.getItem().getMetadata("flag").get(0).value();

                        if (!flag.equals(player.getTeam())) {
                            if (!e.getEntity().hasMetadata("flag"))
                                ((Dodgebolt.CTWGameState) game.getGameState()).pickupFlag(((Player) e.getEntity()), e.getItem());
                        } else {
                            if (e.getEntity().hasMetadata("flag")) {
                                ((Dodgebolt.CTWGameState) game.getGameState()).captureFlag(((Player) e.getEntity()), ((Team) e.getEntity().getMetadata("flag").get(0).value()));
                            }
                            if (e.getItem().hasMetadata("rogue_flag")) {
                                ((Dodgebolt.CTWGameState) game.getGameState()).returnFlag(flag, true);
                            }
                        }
                    }
                }
            }
        }
    }
}
