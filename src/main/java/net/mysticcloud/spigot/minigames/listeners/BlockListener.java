package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.CTW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

public class BlockListener implements Listener {
    public BlockListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getPlayer().hasMetadata("game")) {
            for (MetadataValue value : e.getPlayer().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) {
                        e.setCancelled(true);
                        e.getPlayer().sendMessage(ChatColor.RED + "Sorry, you can't build here.");
                    }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getPlayer().hasMetadata("game")) {
            for (MetadataValue value : e.getPlayer().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) {
                        e.setCancelled(true);
                        e.getPlayer().sendMessage(ChatColor.RED + "Sorry, you can't build here.");
                    }
            }
        }
    }
}
