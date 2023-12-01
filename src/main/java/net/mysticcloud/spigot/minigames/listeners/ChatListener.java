package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    public ChatListener(MysticMinigames plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().hasMetadata("game")) {
            Game game = (Game) e.getPlayer().getMetadata("game").get(0).value();
            assert game != null;
            Team team = game.getPlayers().get(e.getPlayer().getUniqueId());
            game.sendMessage(MessageUtils.colorize("&3" + (team.equals(Team.NONE) ? "" : team.chatColor() + "[&l" + team.name() + "&r" + team.chatColor() + "] ") + e.getPlayer().getName() + "&e: ") + e.getMessage());

            e.setCancelled(true);
        }
    }
}
