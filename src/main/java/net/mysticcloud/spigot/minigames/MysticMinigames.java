package net.mysticcloud.spigot.minigames;


import net.mysticcloud.spigot.minigames.commands.GameCommands;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.plugin.java.JavaPlugin;


public class MysticMinigames extends JavaPlugin {

    public void onEnable() {

        Utils.init(this);


        new GameCommands(this, "game");

    }

    public void onDisable() {

    }

}
