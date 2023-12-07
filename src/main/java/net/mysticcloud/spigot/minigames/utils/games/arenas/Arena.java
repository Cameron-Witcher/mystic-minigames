package net.mysticcloud.spigot.minigames.utils.games.arenas;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.misc.EmptyChunkGenerator;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.metadata.FixedMetadataValue;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Arena {
    String name;
    File masterWorld;
    JSONObject data = new JSONObject("{}");
    private final List<Spawn> spawns = new ArrayList<>();
    private Game game = null;
    private World world = null;

    public Arena(String name, File masterWorld) {
        this.name = name;
        this.masterWorld = masterWorld;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public void setData(JSONObject data) {
        this.data = data;
    }

    public JSONObject getData() {
        return data;

    }

    public void saveToFile() throws IOException {
        File file = new File(masterWorld + "/" + name + ".arena");
        if (!file.exists()) file.createNewFile();
        FileConfiguration yml = YamlConfiguration.loadConfiguration(file);
        yml.set("name", name);
        yml.set("data", data.toString());
        yml.save(file);
    }

    public String getName() {
        return name;
    }

    public File getMasterWorld() {
        return masterWorld;
    }

    public void startGeneration() {
        File newWorld = new File(new Date().getTime() + "");
        newWorld.mkdirs();
        CoreUtils.copyWorld(masterWorld, newWorld);

        WorldCreator wc = new WorldCreator(newWorld.getName());
        wc.generator(new EmptyChunkGenerator());

        world = Bukkit.getServer().createWorld(wc);
    }

    public World getWorld() {
        if (world == null) throw new NullPointerException();
        return world;
    }

    public void addSpawn(Location loc, Team team) {
        spawns.add(new Spawn(loc, team));
        if (!data.has("spawns")) data.put("spawns", new JSONArray());
        JSONObject obj = new JSONObject("{}");
        obj.put("team", team.name());
        obj.put("location", Utils.encryptLocation(loc));
        data.getJSONArray("spawns").put(obj);
    }

    public List<Spawn> getSpawns(Team team) {
        List<Spawn> spawns = new ArrayList<>();
        for (Spawn spawn : Arena.this.spawns)
            if (spawn.getTeam().equals(team)) spawns.add(spawn);
        return spawns;
    }


    public void regenerate() {
        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new Game.GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new Game.GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
            CoreUtils.deleteWorld(world.getWorldFolder());
        }, 1), this::startGeneration), 1), () -> {
            game.getController().generate();
        }), 1);


    }

    public class Spawn {
        Location location;
        Team team;

        public Spawn(Location location) {
            this.location = location;
            this.team = Team.NONE;
        }

        public Spawn(Location location, Team team) {
            this.team = team;
            this.location = location;
        }

        public Team getTeam() {
            return team;
        }

        public Location getLocation() {
            return location.clone();
        }
    }


}