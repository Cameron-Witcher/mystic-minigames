package net.mysticcloud.spigot.minigames.utils.games.arenas;

import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.json2.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ArenaManager {
    static Map<String, Arena> arenas = new HashMap<>();

    public static void registerArenas() {
        //loop thru all arena files, save them to arena object, save arena object to map under file name
        for (File file : Bukkit.getServer().getWorlds().get(0).getWorldFolder().getParentFile().listFiles()) {
            if (file.isDirectory()) {
                for (File file1 : file.listFiles()) {
                    if (file1.getName().endsWith(".arena")) {
                        loadArena(file1);
                    }
                }
            }
        }
    }

    private static void loadArena(File arenaFile) {
        FileConfiguration yml = YamlConfiguration.loadConfiguration(arenaFile);
        Arena arena = new Arena(yml.getString("name"), arenaFile.getParentFile());
        arena.setData(new JSONObject(yml.getString("data")));
        arenas.put(arena.getName(), arena);
    }

    public static Arena getArena(String name) {
        return arenas.getOrDefault(name, null);
    }


    public static Arena createArena(World masterWorld, String name) {
        File file = new File(masterWorld.getWorldFolder() + "/" + name + ".arena");
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
        Arena arena = new Arena(name, masterWorld.getWorldFolder());
        arenas.put(name, arena);
        return arena;
    }
}