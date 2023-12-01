package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game {
    private final String gameName;
    private final Arena arena;
    private final GameState gameState = new GameState();
    private final List<Spawn> spawns = new ArrayList<>();
    private final JSONObject data = new JSONObject("{}");
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private int teams = 0, minPlayers = 2, maxPlayers = 10;
    private GameController controller = null;
    private boolean generated = false;
    private Location lobby = null;


    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
        arena.startGeneration();
        data.put("spawns", new JSONArray());
    }

    public JSONObject getData() {
        return data;
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

    public void removePlayer(UUID uid) {
        players.remove(uid);
        Player player = Bukkit.getPlayer(uid);
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.removeMetadata("game", Utils.getPlugin());
        player.setGameMode(GameMode.SURVIVAL);
    }

    public void end() {

        controller.end();

        for (UUID uid : players.keySet()) {
            Bukkit.getPlayer(uid).teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        players.clear();
        close();
        GameManager.removeGame(gameName + "-" + arena.getName());
        GameManager.createGame(gameName, arena, teams, data);
    }

    public boolean addPlayer(UUID uid) {
        if (players.size() >= maxPlayers || !gameState.acceptingPlayers()) return false;

        Player player = Bukkit.getPlayer(uid);
        BukkitTask task = null;
        if (!generated) {
            player.sendMessage(MessageUtils.prefixes("game") + "Generating world... Please wait.");
            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                controller.generate();
            }, 0), () -> {
                JSONArray save = RegionUtils.getSave("lobby");
                Location loc = new Location(arena.getWorld(), arena.getLength() / 2, arena.getHeight() + 1, arena.getWidth() / 2);
                for (int i = 0; i < save.length(); i++) {
                    JSONObject data = save.getJSONObject(i);

                    if (!Bukkit.createBlockData(data.getString("data")).getMaterial().equals(Material.STRUCTURE_BLOCK)) {
                        loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z")).getBlock().setBlockData(Bukkit.createBlockData(data.getString("data")));
                    } else {
                        Location bloc = loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                        bloc.getBlock().setType(Material.AIR);
                        JSONObject sdata = data.getJSONObject("structure_data");
                        switch (sdata.getString("structure")) {
                            case "lobby:spawn":
                                lobby = bloc;
                                break;

                        }
                    }
                }
            }), 0);
            generated = true;
        }
        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(task, () -> {
            player.teleport(lobby);
            player.setMetadata("game", new FixedMetadataValue(Utils.getPlugin(), this));
            players.put(player.getUniqueId(), new GamePlayer(player.getUniqueId()));
            sendMessage("&3" + player.getName() + "&e has joined! (&3" + players.size() + "&e/&3" + maxPlayers + "&e)");
            if (players.size() >= minPlayers && !gameState.countdown()) {
                gameState.startCountdown();

            }
        }), 0);


        return true;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void sendMessage(String message) {
        for (Team team : Team.values())
            sendMessage(team, message);
    }

    public void startGame() {
        lobby = null;
        JSONArray save = RegionUtils.getSave("lobby");
        Location loc = new Location(arena.getWorld(), arena.getLength() / 2, arena.getHeight() + 1, arena.getWidth() / 2);
        for (int i = 0; i < save.length(); i++) {
            JSONObject data = save.getJSONObject(i);
            loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z")).getBlock().setType(Material.AIR);

        }
        gameState.hasStarted(true);
        controller.start();
    }


    public void sendMessage(Team team, String message) {
        for (Map.Entry<UUID, GamePlayer> e : players.entrySet()) {
            if (e.getValue().getTeam().equals(team))
                Bukkit.getPlayer(e.getKey()).sendMessage(MessageUtils.colorize(message));
        }
    }

    public void close() {
        generated = false;
        arena.clear();
    }

    public Map<UUID, GamePlayer> getPlayers() {
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
        json.put("extra", data);
        return json;
    }

    public GameController getController() {
        return controller;
    }

    public void kill(Player player, EntityDamageEvent.DamageCause cause) {
        GamePlayer gamePlayer = players.get(player.getUniqueId());
        gamePlayer.setLives(gamePlayer.getLives() - 1);
        player.setHealth(player.getHealthScale());
        if (gamePlayer.getLives() > 0) spawnPlayer(player);
        if (gamePlayer.getLives() == 0) {
            setSpectator(player);
        }
    }

    private void setSpectator(Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        getPlayer(player.getUniqueId()).setTeam(Team.SPECTATOR);
        if (player.getLocation().getY() < 0)
            player.teleport(player.getLocation().clone().add(0, Math.abs(player.getLocation().getY()) + 50, 0));
    }

    public GamePlayer getPlayer(UUID uid) {
        return players.get(uid);
    }

    public void addSpawn(Spawn spawn) {
        spawns.add(spawn);
    }

    protected void spawnPlayer(Player player) {
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        player.setHealth(player.getHealthScale());
        for (Spawn spawn : spawns)
            if (spawn.getTeam().equals(gamePlayer.getTeam())) {
                player.teleport(spawn.getLocation());
                break;
            }
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


    public class GameState {

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
            if (!countdown) {
                countdown = true;
                Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new CountdownTimer(new Date().getTime(), 10), 0);
            }

        }

        public boolean hasStarted() {
            return gameRunning;
        }

        public void hasStarted(boolean gameRunning) {
            this.gameRunning = gameRunning;
        }
    }

    private class GenerateRunnable implements Runnable {
        BukkitTask task;
        Runnable run;

        GenerateRunnable(BukkitTask task, Runnable run) {
            this.run = run;
            this.task = task;
        }


        @Override
        public void run() {
            if (task == null || !Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId())) {
                run.run();

            } else Bukkit.getScheduler().runTaskLaterAsynchronously(Utils.getPlugin(), this, 0);
        }
    }

    private class CountdownTimer implements Runnable {

        long date;
        int timer;

        CountdownTimer(long date, int timer) {
            this.timer = timer;
            this.date = date;
        }

        @Override
        public void run() {
            if (new Date().getTime() - date >= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)) {
                date = new Date().getTime();
                sendMessage(MessageUtils.colorize("&3Starting in " + timer + " second" + (timer == 1 ? "" : "s") + "!"));
                timer = timer - 1;

            }
            if (timer != 0) {
                Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), this, 1);
            } else startGame();
        }
    }

    protected interface GameController {

        public void start();

        public boolean check();

        public void end();

        public void generate();
    }

    public class GamePlayer {
        Team team = Team.NONE;
        int lives = 3;
        UUID uid;

        GamePlayer(UUID uid) {
            this.uid = uid;
        }

        public Team getTeam() {
            return team;
        }

        public void setTeam(Team team) {
            this.team = team;
        }

        public int getLives() {
            return lives;
        }

        public void setLives(int lives) {
            this.lives = lives;
        }

        public UUID getUUID() {
            return uid;
        }
    }
}