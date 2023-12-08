package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.json2.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game {
    private final String gameName;
    private final Arena arena;
    private final GameState gameState = new GameState();
    private final JSONObject data = new JSONObject("{}");
    private final Map<UUID, Integer> playerScores = new HashMap<>();
    private final Map<Team, Integer> teamScores = new HashMap<>();
    private final Map<UUID, GamePlayer> players = new HashMap<>();
    private final List<Location> noBuildZones = new ArrayList<>();
    private final GameScoreboard gameScoreboard = new GameScoreboard();
    private int teams = 0, minPlayers = 2, maxPlayers = 10;
    private GameController controller = null;
    private boolean generated = false;


    private Map<UUID, ItemStack[]> inventoryList = new HashMap<>();


    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
        arena.setGame(this);
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
        player.setFallDistance(0);
        player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().setContents(inventoryList.get(player.getUniqueId()));
        player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        inventoryList.remove(player.getUniqueId());
    }

    public void end() {
        if(!gameState.isEnding()) {
            gameState.startEnding();

            for (UUID uid : players.keySet()) {
                Player player = Bukkit.getPlayer(uid);
                player.setGameMode(GameMode.SPECTATOR);
                getPlayer(player.getUniqueId()).setTeam(Team.SPECTATOR);
            }


            controller.end();

            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                for (UUID uid : players.keySet()) {
                    removePlayer(uid, false);
                }
                players.clear();
                teamScores.clear();
                playerScores.clear();
                arena.delete();

                generated = false;
                gameState.reset();
                gameScoreboard.reset();
                noBuildZones.clear();
            }, 15 * 20);
        }

    }

    public boolean addPlayer(UUID uid) {
        if (players.size() >= maxPlayers || !gameState.acceptingPlayers()) {
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.prefixes("game") + "Sorry, that game is either full or has already started.");
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize("&7Spectate games coming soon."));
            return false;
        }

        Player player = Bukkit.getPlayer(uid);
        inventoryList.put(uid, player.getInventory().getContents());
        BukkitTask task = null;

        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new GenerateRunnable(Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
            if (!generated) {
                player.sendMessage(MessageUtils.prefixes("game") + "Generating world... Please wait.");
                generate();
            }
        }, 0), () -> {
            try {
                List<Arena.Spawn> spawns = arena.getSpawns(Team.SPECTATOR);
                player.teleport(spawns.get(new Random().nextInt(spawns.size())).getLocation());
            } catch (IllegalArgumentException ex) {
                List<Arena.Spawn> spawns = arena.getSpawns(Team.NONE);
                player.teleport(spawns.get(new Random().nextInt(spawns.size())).getLocation());
            }
            player.setScoreboard(gameScoreboard.getScoreboard());
            players.put(player.getUniqueId(), new GamePlayer(player.getUniqueId()));
            player.setGameMode(GameMode.SPECTATOR);
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


    public void startGame() {
        for (GamePlayer player : players.values()) {
            teamScores.put(player.getTeam(), 0);
            playerScores.put(player.getUUID(), 0);
        }
        gameState.hasStarted(true);
        controller.start();

    }

    public void sendMessage(String message) {
        for (Team team : Team.values())
            sendMessage(team, message);
    }


    public void sendMessage(Team team, String message) {
        for (UUID uid : getPlayers(team))
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize(message));
    }

    public void close() {
        generated = false;
        arena.regenerate();
        generated = true;
    }

    public Map<UUID, GamePlayer> getPlayers() {
        return players;
    }

    public List<UUID> getPlayers(Team team) {
        List<UUID> players = new ArrayList<>();
        for (Map.Entry<UUID, GamePlayer> entry : this.players.entrySet()) {
            if (entry.getValue().getTeam().equals(team)) players.add(entry.getKey());
        }
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
                return false;
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
        player.getWorld().strikeLightningEffect(player.getLocation());
        getPlayer(player.getUniqueId()).setTeam(Team.SPECTATOR);
        if (player.getLocation().getY() < 0)
            player.teleport(player.getLocation().clone().add(0, Math.abs(player.getLocation().getY()) + 50, 0));
    }

    public GamePlayer getPlayer(UUID uid) {
        return players.get(uid);
    }


    protected void spawnPlayer(Player player) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        player.setHealth(player.getHealthScale());
        List<Arena.Spawn> spawns = arena.getSpawns(gamePlayer.getTeam());
        player.teleport(spawns.get(new Random().nextInt(spawns.size())).getLocation());
    }


    public void generate() {
        arena.startGeneration();
        arena.getWorld().setMetadata("game", new FixedMetadataValue(Utils.getPlugin(), this));
        controller.generate();
        generated = true;

    }

    protected Firework spawnFirework(Location loc, FireworkEffect effect) {
        Firework rocket = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta meta = rocket.getFireworkMeta();
        meta.clearEffects();
        meta.addEffect(effect);
        rocket.setFireworkMeta(meta);
        rocket.setMetadata("game", new FixedMetadataValue(Utils.getPlugin(), this));
        return rocket;
    }

    public GameScoreboard getScoreboardManager() {
        return gameScoreboard;
    }


    public class GameState {

        boolean lobbyOpen = true;
        boolean gameRunning = false;
        boolean countdown = false;
        boolean ending = false;
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
                    if (getPlayers().size() < minPlayers) {
                        sendMessage(MessageUtils.colorize("&cNot enough players. Cancelling countdown."));
                        return true;
                    }
                    sendMessage(MessageUtils.colorize("&3Starting in " + t + " second" + (t == 1 ? "" : "s") + "!"));
                    for (UUID uid : getPlayers().keySet()) {
                        Player player = Bukkit.getPlayer(uid);
                        if (t <= 5) {
                            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER, 1f, 0.5f);

                        }
                    }
                    return false;
                }, () -> {
                    startGame();
                    for (UUID uid : getPlayers().keySet()) {
                        Player player = Bukkit.getPlayer(uid);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 0.5f);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 1f, 0.5f);
                    }

                }), 0);
            }

        }

        public boolean hasStarted() {
            return gameRunning;
        }

        public void hasStarted(boolean gameRunning) {
            this.gameRunning = gameRunning;
        }

        public void startEnding(){
            ending = true;
        }

        public boolean isEnding(){
            return ending;
        }

        public void reset() {
            lobbyOpen = true;
            gameRunning = false;
            countdown = false;
            started = 0;
            ending = false;
        }
    }

    public static class GenerateRunnable implements Runnable {
        BukkitTask task;
        Runnable run;

        public GenerateRunnable(BukkitTask task, Runnable run) {
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
        TimerTick tick;
        boolean cancel = false;

        CountdownRunnable(int timer, TimerTick tick, Runnable finish) {
            this.timer = timer;
            this.date = new Date().getTime();
            this.finish = finish;
            this.tick = tick;
            tick();
        }

        private void tick() {
            cancel = tick.go(timer);
            timer = timer - 1;
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
            if (!cancel) Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), this, 1);
        }
    }


    private interface TimerTick {
        public abstract boolean go(int timer);
    }


    public interface GameController {

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

    public class GameScoreboard {
        ScoreboardManager scoreboardManager;
        Scoreboard scoreboard;


        public GameScoreboard() {
            setup();
        }

        public Scoreboard getScoreboard() {
            return scoreboard;
        }

        public void reset() {
            setup();
        }

        private void setup() {
            scoreboardManager = Bukkit.getScoreboardManager();
            scoreboard = scoreboardManager.getNewScoreboard();
            for (Team team : Team.values())
                scoreboard.registerNewTeam(team.name()).setColor(team.chatColor());
        }
    }
}