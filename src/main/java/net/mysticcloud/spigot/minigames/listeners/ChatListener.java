package net.mysticcloud.spigot.minigames.listeners;

import net.mysticcloud.spigot.core.MysticCore;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    public ChatListener(MysticCore plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent e) {
        if (e.getPlayer().hasMetadata("game")) {
            Game game = (Game) e.getPlayer().getMetadata("game").get(0).value();
            assert game != null;
            if (!game.getGameState().hasStarted()) {
                game.sendMessage(MessageUtils.colorize("&3" + e.getPlayer().getName() + "&e: ") + e.getMessage());
            }
            e.setCancelled(true);
        }
    }
}
