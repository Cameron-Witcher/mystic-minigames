package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;

public class Utils {

    private static MysticMinigames plugin;

    public static void init(MysticMinigames mainClass) {
        plugin = mainClass;
        GameManager.init();

        CoreUtils.addPalpitation(() -> {
            for (Game game : GameManager.getGames().values()) {
                if (game.getController().check()) game.end();
            }
        });
    }

    public static MysticMinigames getPlugin() {
        return plugin;
    }
}
