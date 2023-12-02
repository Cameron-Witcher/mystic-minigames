package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.List;

public class BlockListener implements Listener {
    public BlockListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockChange(BlockFromToEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) e.setCancelled(true);

            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockBreakEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) {
                        e.setCancelled(true);
                        if (e.getPlayer() != null)
                            e.getPlayer().sendMessage(MessageUtils.colorize("&3Sorry, you can't edit blocks right here."));
                    }
            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockPlaceEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) {
                        e.setCancelled(true);
                        if (e.getPlayer() != null)
                            e.getPlayer().sendMessage(MessageUtils.colorize("&3Sorry, you can't edit blocks right here."));
                    }
            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockExplodeEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones()) {
                    List<Block> remove = new ArrayList<>();
                    for (Block block : e.blockList())
                        if (CoreUtils.distance(block.getLocation(), location) <= 5) remove.add(block);
                    e.blockList().removeAll(remove);
                    remove.clear();
                }

            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockBurnEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockIgniteEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                for (Location location : game.getNoBuildZones())
                    if (CoreUtils.distance(location, e.getBlock().getLocation()) <= 5) {
                        e.setCancelled(true);
                        if (e.getPlayer() != null)
                            e.getPlayer().sendMessage(MessageUtils.colorize("&3Sorry, you can't edit blocks right here."));
                    }
            }
        }
    }
}
