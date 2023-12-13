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
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.Arrays;

public class ServerListener implements Listener {
    public ServerListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        e.setJoinMessage(null);
        Utils.getScoreboardManager(e.getPlayer().getUniqueId()).set();
        for (Player player : Bukkit.getOnlinePlayers())
            if (!player.getWorld().hasMetadata("game"))
                player.sendMessage(MessageUtils.colorize("&a[+] " + e.getPlayer().getName()));

    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        e.setQuitMessage(null);
        e.getPlayer().setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        for (Player player : Bukkit.getOnlinePlayers())
            if (!player.getWorld().hasMetadata("game"))
                player.sendMessage(MessageUtils.colorize("&c[-] " + e.getPlayer().getName()));
        if (e.getPlayer().getWorld().hasMetadata("game")) {
            for (MetadataValue metadataValue : e.getPlayer().getWorld().getMetadata("game")) {
                Game game = (Game) metadataValue.value();
                game.getGameState().removePlayer(e.getPlayer().getUniqueId());
            }
        }
    }
}
