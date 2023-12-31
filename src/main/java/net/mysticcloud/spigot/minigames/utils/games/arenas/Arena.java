package net.mysticcloud.spigot.minigames.utils.games.arenas;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.misc.EmptyChunkGenerator;
import net.mysticcloud.spigot.core.utils.world.WorldManager;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
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
    private Game game = null;
    private World world = null;

    public Arena(String name, File masterWorld) {
        this.name = name;
        this.masterWorld = masterWorld;
    }

    public void setGame(Game game) {
        this.game = game;
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
        File newWorld = new File(name + "_" + new Date().getTime());
        newWorld.mkdirs();
//        WorldManager.copyWorld(masterWorld, newWorld);
        CoreUtils.copyWorld(masterWorld, newWorld);

        WorldCreator wc = new WorldCreator(newWorld.getName());
//        wc.generator(new EmptyChunkGenerator());

        world = Bukkit.getServer().createWorld(wc);
        world.setDifficulty(Difficulty.HARD);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DISABLE_RAIDS, true);
        if (data.has("world_time")) world.setTime(data.getLong("world_time"));
        else world.setTime(6000);
        if (data.has("storm")) {
            world.setStorm(true);
            world.setWeatherDuration(1000000000);
            world.setThundering(data.getString("storm").equalsIgnoreCase("thunder"));

        } else {
            world.setStorm(false);
            world.setWeatherDuration(0);
            world.setThundering(false);
            world.setClearWeatherDuration(1000000000);
        }

    }

    public World getWorld() {
        if (world == null) throw new NullPointerException();
        return world;
    }

    public void addSpawn(Location loc, Team team) {
        checkSpawns();
        JSONObject obj = new JSONObject("{}");
        obj.put("team", team.name());
        obj.put("location", Utils.encryptLocation(loc));
        data.getJSONArray("spawns").put(obj);
    }

    public List<Spawn> getSpawns() {
        List<Spawn> spawns = new ArrayList<>();
        checkSpawns();
        for (int i = 0; i < data.getJSONArray("spawns").length(); i++) {
            JSONObject spawnData = data.getJSONArray("spawns").getJSONObject(i);
            spawns.add(new Spawn(Utils.decryptLocation(world, spawnData.getJSONObject("location")), spawnData.has("team") ? Team.valueOf(spawnData.getString("team").toUpperCase()) : Team.NONE));
        }
        return spawns;
    }

    private void checkSpawns() {
        if (!data.has("spawns")) data.put("spawns", new JSONArray());
    }

    public List<Spawn> getSpawns(Team team) {
        List<Spawn> spawns = new ArrayList<>();
        for (Spawn spawn : getSpawns())
            if (spawn.getTeam().equals(team)) spawns.add(spawn);
        return spawns;
    }


    public void regenerate() {
        WorldManager.deleteWorld(world);
//        CoreUtils.deleteWorld(world.getWorldFolder());
        startGeneration();
        game.getController().generate();


    }

    void setData(JSONObject data) {
        this.data = data;
    }

    public void delete() {
        if (world != null) WorldManager.deleteWorld(world);
        world = null;
    }

    public static class Spawn {
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