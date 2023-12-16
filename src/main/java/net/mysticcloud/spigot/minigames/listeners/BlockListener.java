package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public void onBlockChange(EntityExplodeEvent e) {
        if (e.getEntity().getWorld().hasMetadata("game")) {
            e.setCancelled(true);
            for (MetadataValue value : e.getEntity().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                List<Block> remove = new ArrayList<>();
                for (Location location : game.getNoBuildZones()) {
                    for (Block block : e.blockList())
                        if (CoreUtils.distance(block.getLocation(), location) >= 5) remove.add(block);
                }
                e.blockList().removeAll(remove);
                game.explodeBlocks(e.blockList());

            }
        }
    }

    @EventHandler
    public void onBlockChange(BlockExplodeEvent e) {
        if (e.getBlock().getWorld().hasMetadata("game")) {
            e.setCancelled(true);
            for (MetadataValue value : e.getBlock().getWorld().getMetadata("game")) {
                Game game = (Game) value.value();
                List<Block> remove = new ArrayList<>();
                for (Location location : game.getNoBuildZones()) {
                    for (Block block : e.blockList())
                        if (CoreUtils.distance(block.getLocation(), location) <= 5) remove.add(block);
                }
                e.blockList().removeAll(remove);
                game.explodeBlocks(e.blockList());

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
