package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Game {
    private String gameName;
    private Arena arena;
    private final GameState gameState = new GameState();
    Map<UUID, Team> players = new HashMap<>();
    private int teams = 0;
    private int minPlayers = 2;
    private int maxPlayers = 10;
    private GameController controller = null;
    private Location lobby = null;

    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
    }

    protected void setTeams(int i) {
        teams = i;
    }

    protected void setMinPlayers(int i) {
        minPlayers = i;
    }

    protected void setMaxPlayers(int i) {
        maxPlayers = i;
    }

    protected void setController(GameController controller) {
        this.controller = controller;
    }

    public boolean addPlayer(UUID uid) {
        if (players.size() >= maxPlayers || !gameState.acceptingPlayers()) return false;
        Player player = Bukkit.getPlayer(uid);
        if (arena.getWorld() == null) {
            player.sendMessage(MessageUtils.prefixes("game") + "Generating world... Please wait.");
            generate();
        }
        player.teleport(lobby);
        players.put(uid, Team.NONE);
        sendMessage("&3" + player.getName() + "&e has joined! (&3" + players.size() + "&e/&3" + maxPlayers + "&e)");
        if (players.size() >= minPlayers && !gameState.countdown()) {
            gameState.startCountdown();

        }
        return true;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void sendMessage(String message) {
        for (Team team : Team.values())
            sendMessage(team, message);
    }

    public void generate() {
        arena.startGeneration();
        controller.generate();
        JSONArray save = RegionUtils.getSave("lobby");
        Location loc = new Location(arena.getWorld(), arena.getLength() / 2, arena.getHeight() + 1, arena.getWidth() / 2);
        for (int i = 0; i < save.length(); i++) {
            JSONObject data = save.getJSONObject(i);

            if (!Bukkit.createBlockData(data.getString("data")).getMaterial().equals(Material.STRUCTURE_BLOCK)) {
                loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z")).getBlock().setBlockData(Bukkit.createBlockData(data.getString("data")));
            } else {
                Location bloc = loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                JSONObject sdata = data.getJSONObject("structure_data");
                switch (sdata.getString("structure")) {
                    case "lobby:spawn":
                        lobby = bloc;
                        break;

                }
            }
        }

        //Do shit
        RegionUtils.pasteSave("lobby", new Location(arena.getWorld(), arena.getLength() / 2, arena.getHeight() + 1, arena.getWidth() / 2));
    }


    public void sendMessage(Team team, String message) {
        for (Map.Entry<UUID, Team> e : players.entrySet()) {
            if (e.getValue().equals(team)) Bukkit.getPlayer(e.getKey()).sendMessage(MessageUtils.colorize(message));
        }
    }

    public void close() {
        arena.clear();
    }

    public Map<UUID, Team> getPlayers() {
        return players;
    }

    public int getTeams() {
        return teams;
    }

    public String getName() {
        return gameName;
    }

    public Arena getArena() {
        return arena;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject("{}");
        json.put("game", gameName);
        json.put("arena", arena.getName());
        if (teams > 1) json.put("teams", teams);
        return json;
    }

    public GameController getController() {
        return controller;
    }


    static class GameState {

        boolean lobbyOpen = true;
        boolean gameRunning = false;
        boolean countdown = false;
        long started = 0;

        public boolean acceptingPlayers() {
            return lobbyOpen && !gameRunning;
        }

        public boolean countdown() {
            return countdown;
        }

        public void startCountdown() {
            if (!countdown) countdown = true;

        }
    }

    protected interface GameController {

        public void start();

        public boolean check();

        public void end();

        public void generate();
    }
}