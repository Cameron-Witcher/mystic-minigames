package net.mysticcloud.spigot.minigames.utils.games.arenas;

import net.mysticcloud.spigot.core.utils.misc.EmptyChunkGenerator;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.Date;

public class Arena {
    String name;
    World world = null;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void startGeneration() {

        WorldCreator wc = new WorldCreator(new Date().getTime() + "");
        wc.generator(new EmptyChunkGenerator());
        world = wc.createWorld();
    }

    public void clear() {
        Bukkit.getServer().unloadWorld(world, true);
        deleteWorld(world.getWorldFolder());
        world = null;
    }

    private boolean deleteWorld(File path) {
        if (path.exists()) {
            File files[] = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteWorld(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public World getWorld() {
        return world;
    }
}