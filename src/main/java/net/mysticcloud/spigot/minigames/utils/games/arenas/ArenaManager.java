package net.mysticcloud.spigot.minigames.utils.games.arenas;

import java.util.HashMap;
import java.util.Map;

public class ArenaManager {
    static Map<String, Arena> arenas = new HashMap<>();

    public static void registerArenas() {
        Arena arena = new Arena("test");
        arenas.put("test", arena);
        //loop thru all arena files, save them to arena object, save arena object to map under file name
    }
}