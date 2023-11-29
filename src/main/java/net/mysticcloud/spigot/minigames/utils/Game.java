package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Structure;
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
        //trash code for tests
        players.put(uid, Team.NONE);
        sendMessage("&3" + Bukkit.getPlayer(uid).getName() + "&e has joined! (&3" + players.size() + "&e/&3" + maxPlayers + "&e)");
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
        if (teams > 1)
            json.put("teams", teams);
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