package net.mysticcloud.spigot.minigames;


import net.mysticcloud.spigot.minigames.commands.GameCommands;
import net.mysticcloud.spigot.minigames.listeners.ChatListener;
import net.mysticcloud.spigot.minigames.listeners.DeathListener;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.GameManager;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.plugin.java.JavaPlugin;


public class MysticMinigames extends JavaPlugin {

    public void onEnable() {

        Utils.init(this);


        new ChatListener(this);
        new DeathListener(this);
        new GameCommands(this, "game");

    }

    public void onDisable() {

        for (Game game : GameManager.getGames().values()) {
            game.end();
            GameManager.saveGame(game);
        }

    }

}
