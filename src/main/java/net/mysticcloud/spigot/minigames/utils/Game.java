package net.mysticcloud.spigot.minigames.utils;

import jdk.internal.org.objectweb.asm.tree.InsnList;
import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game {
    private final String gameName;
    private final Arena arena;
    private final GameState gameState = new GameState();
    private final List<Spawn> spawns = new ArrayList<>();
    private final JSONObject data = new JSONObject("{}");
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<Team, Integer> teamScores = new HashMap<>();
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private int teams = 0, minPlayers = 2, maxPlayers = 10;
    private GameController controller = null;
    private boolean generated = false;
    private Location lobby = null;
    private List<Location> noBuildZones = new ArrayList<>();


    private Map<UUID, ItemStack[]> inventoryList = new HashMap<>();


    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
        data.put("spawns", new JSONArray());
        generate();
    }

    public JSONObject getData() {
        return data;
    }

    public void addNoBuildZone(Location location) {
        noBuildZones.add(location);
    }

    public List<Location> getNoBuildZones() {
        return noBuildZones;
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
        removePlayer(uid, true);
    }

    public void removePlayer(UUID uid, boolean list) {
        if (list) players.remove(uid);
        Player player = Bukkit.getPlayer(uid);
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().setContents(inventoryList.get(player.getUniqueId()));
        inventoryList.remove(player.getUniqueId());
    }

    public void end() {

        controller.end();

        for (UUID uid : players.keySet()) {
            removePlayer(uid, false);
        }
        players.clear();
        close();
        GameManager.removeGame(gameName + "-" + arena.getName());
        GameManager.createGame(gameName, arena, teams, data);
    }

    public boolean addPlayer(UUID uid) {
        if (players.size() >= maxPlayers || !gameState.acceptingPlayers()) return false;

        Player player = Bukkit.getPlayer(uid);
        inventoryList.put(uid, player.getInventory().getContents());
        BukkitTask task = null;
        if (!generated) {
            player.sendMessage(MessageUtils.prefixes("game") + "Generating world... Please wait.");
            generate();
        }
        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(task, () -> {
            player.teleport(lobby);
            players.put(player.getUniqueId(), new GamePlayer(player.getUniqueId()));
            sendMessage("&3" + player.getName() + "&e has joined! (&3" + players.size() + "&e/&3" + maxPlayers + "&e)");
            if (players.size() >= minPlayers && !gameState.countdown()) {
                gameState.startCountdown();

            }
        }), 0);


        return true;
    }

    public int score(Player player) {
        return score(player, 1);
    }

    public int score(Player player, int amount) {
        playerScores.put(player.getUniqueId(), getScore(player) + amount);
        return playerScores.get(player.getUniqueId());
    }

    public int score(Team team) {
        return score(team, 1);
    }

    public int score(Team team, int amount) {
        return teamScores.put(team, getScore(team) + amount);
    }

    public int getScore(Player player) {
        if (!playerScores.containsKey(player.getUniqueId())) playerScores.put(player.getUniqueId(), 0);
        return playerScores.get(player.getUniqueId());
    }

    public int getScore(Team team) {
        if (!teamScores.containsKey(team)) teamScores.put(team, 0);
        return teamScores.get(team);
    }

    public Map<Team, Integer> getTeamScores() {
        return teamScores;
    }

    public Map<UUID, Integer> getPlayerScores() {
        return playerScores;
    }

    public LinkedHashMap<Team, Integer> sortTeamScores() {
        List<Map.Entry<Team, Integer>> list = new LinkedList<Map.Entry<Team, Integer>>(teamScores.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Team, Integer>>() {

            @Override
            public int compare(Map.Entry<Team, Integer> o1, Map.Entry<Team, Integer> o2) {
                // TODO Auto-generated method stub
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        LinkedHashMap<Team, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<Team, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }

        return temp;

    }

    public LinkedHashMap<UUID, Integer> sortPlayerScores() {
        List<Map.Entry<UUID, Integer>> list = new LinkedList<Map.Entry<UUID, Integer>>(playerScores.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<UUID, Integer>>() {

            @Override
            public int compare(Map.Entry<UUID, Integer> o1, Map.Entry<UUID, Integer> o2) {
                // TODO Auto-generated method stub
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        LinkedHashMap<UUID, Integer> temp = new LinkedHashMap<>();
        for (Map.Entry<UUID, Integer> aa : list) {
            temp.put(aa.getKey(), aa.getValue());
        }

        return temp;

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
        if (gamePlayer.getLives() > 0) {
            player.setGameMode(GameMode.SPECTATOR);
            if (player.getLocation().getY() < 0)
                player.teleport(player.getLocation().clone().add(0, Math.abs(player.getLocation().getY()) + 50, 0));
            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new CountdownRunnable(3, (timer) -> {
                player.sendTitle("", ChatColor.RED + "Respawning in " + timer + " second" + (timer == 1 ? "" : "s"), 0, 25, 50);
            }, () -> {
                player.setGameMode(GameMode.SURVIVAL);
                spawnPlayer(player);
            }), 0);
        }
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
        player.getInventory().clear();
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        player.setHealth(player.getHealthScale());
        List<Spawn> spawns = getSpawns(gamePlayer.getTeam());
        player.teleport(spawns.get(new Random().nextInt(spawns.size())).getLocation());
    }

    private List<Spawn> getSpawns(Team team) {
        List<Spawn> spawns = new ArrayList<>();
        for (Spawn spawn : Game.this.spawns)
            if (spawn.getTeam().equals(team)) spawns.add(spawn);
        return spawns;
    }

    public void generate() {
        arena.startGeneration();
        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
            arena.startGeneration();
            arena.getWorld().setMetadata("game", new FixedMetadataValue(Utils.getPlugin(), this));
        }, 1), () -> {
            controller.generate();
        }), 1), () -> {
            generateLobby();
        }), 1);
        generated = true;
    }

    private void generateLobby() {
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
                Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new CountdownRunnable(10, (int t) -> {
                    sendMessage(MessageUtils.colorize("&3Starting in " + t + " second" + (t == 1 ? "" : "s") + "!"));
                }, () -> {
                    startGame();
                }), 0);
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

    private class CountdownRunnable implements Runnable {

        long date;
        int timer;
        Runnable finish;
        TimerRunnable tick;

        CountdownRunnable(int timer, TimerRunnable tick, Runnable finish) {
            this.timer = timer;
            this.date = new Date().getTime();
            this.finish = finish;
            this.tick = tick;
            tick();
        }

        @Override
        public void run() {
            if (new Date().getTime() - date >= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)) {
                if (timer == 0) {
                    finish.run();
                    return;
                }
                date = new Date().getTime();
                tick();

            }
            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), this, 1);

        }

        private void tick() {
            tick.go(timer);
            timer = timer - 1;
        }

    }

    private interface TimerRunnable {
        public abstract void go(int timer);
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

        public void setMaxLives(int lives) {
            this.lives = lives;
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