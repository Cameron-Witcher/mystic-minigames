package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.accounts.AccountManager;
import net.mysticcloud.spigot.core.utils.accounts.MysticPlayer;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;

import net.mysticcloud.spigot.minigames.utils.misc.ScoreboardManager;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitTask;
import org.json2.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Game {
    private final String gameName;
    private final Arena arena;
    private GameState gameState = new GameState();
    private final JSONObject data = new JSONObject("{}");
    private final List<Location> noBuildZones = new ArrayList<>();
    private final Map<UUID, ScoreboardManager> scoreboards = new HashMap<UUID, ScoreboardManager>();
    private final Map<UUID, ItemStack[]> inventoryList = new HashMap<>();
    private int teams = 0, minPlayers = 2, maxPlayers = 10;
    private GameController controller = null;
    private boolean generated = false;
    private boolean friendlyFire = true;


    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
        arena.setGame(this);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setFriendlyFire(boolean friendlyFire) {
        this.friendlyFire = friendlyFire;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
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


    public GameState getGameState() {
        return gameState;
    }


    public void sendMessage(String message) {
        for (Team team : Team.values())
            sendMessage(team, message);
    }


    public void sendMessage(Team team, String message) {
        for (UUID uid : getGameState().getPlayers(team))
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize(message));
    }

    public void close() {
        generated = false;
        arena.regenerate();
        generated = true;
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

    public Map<UUID, ScoreboardManager> getScoreboards() {
        return scoreboards;
    }

    public GameController getController() {
        return controller;
    }


    public void defaultDeathMessages(Player player, EntityDamageEvent.DamageCause cause) {
        GamePlayer gamePlayer = gameState.getPlayer(player.getUniqueId());
        Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;

        String victim = (gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName();
        String action = " was killed";
        String ending = "!";
        switch (cause) {
            case PROJECTILE:
                action = " was shot";
                ending = (entity == null ? " by a projectile!" : " by " + (entity instanceof Player ? (gameState.getPlayer(entity.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : gameState.getPlayer(entity.getUniqueId()).getTeam().chatColor()) : "&7") + entity.getName() + "&e!&7 (" + CoreUtils.distance(player.getLocation(), entity.getLocation()).intValue() + " blocks)");
                break;
            case VOID:
                action = " fell out of the world";
                ending = ".";
                if (entity != null) {
                    Player killer = (Player) entity;
                    action = " was pushed over the edge";
                    ending = " by " + (gameState.getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : gameState.getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                }
                break;
            default:
                if (entity != null) {
                    ending = " by &7" + entity.getName() + "&e.";
                    if (entity instanceof Player) {
                        Player killer = (Player) entity;
                        if (killer.getEquipment() != null && killer.getEquipment().getItemInMainHand().getType().name().endsWith("_AXE")) {
                            action = " was decapitated";
                        }
                        ending = " by " + (gameState.getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : gameState.getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                    }
                }
                break;
        }
        sendMessage("&3" + victim + "&e" + action + ending);
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



    public class GameState {

        private Map<UUID, Integer> playerScores = new HashMap<>();
        private Map<Team, Integer> teamScores = new HashMap<>();
        private Map<UUID, GamePlayer> players = new HashMap<>();

        boolean lobbyOpen = true;
        boolean gameRunning = false;
        boolean countdown = false;
        boolean ending = false;
        long STARTED = 0;


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
                    start();
                    for (UUID uid : getPlayers().keySet()) {
                        Player player = Bukkit.getPlayer(uid);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 1f, 0.5f);
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER, 1f, 0.5f);
                    }

                }, ()->{
                    countdown = false;
                }), 0);
            }

        }

        public boolean hasStarted() {
            return gameRunning;
        }

        public void start() {
            controller.start();
            STARTED = new Date().getTime();
            for (GamePlayer player : gameState.getPlayers().values()) {
                if (!player.getTeam().equals(Team.NONE) && !player.getTeam().equals(Team.SPECTATOR))
                    teamScores.put(player.getTeam(), 0);
                Bukkit.getPlayer(player.getUUID()).setMetadata("original_team", new FixedMetadataValue(Utils.getPlugin(), player.getTeam()));
                playerScores.put(player.getUUID(), 0);
            }
            this.gameRunning = true;

        }

        public void startEnding() {
            ending = true;
        }

        public boolean isEnding() {
            return ending;
        }

        public void removePlayer(UUID uid) {
            removePlayer(uid, true);
        }

        public void removePlayer(UUID uid, boolean list) {
            if (list) players.remove(uid);
            Player player = Bukkit.getPlayer(uid);
            player.setFallDistance(0);
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.setFoodLevel(20);
            player.setGameMode(GameMode.SURVIVAL);
            if (inventoryList.containsKey(player.getUniqueId()))
                player.getInventory().setContents(inventoryList.get(player.getUniqueId()));
            Utils.getScoreboardManager(uid).set();
            inventoryList.remove(player.getUniqueId());
            player.removeMetadata("original_team", Utils.getPlugin());
        }

        public List<UUID> getPlayers(Team team) {
            List<UUID> uids = new ArrayList<>();
            for (GamePlayer gamePlayer : gameState.getPlayers().values()) {
                if (gamePlayer.getTeam().equals(team)) uids.add(gamePlayer.getUUID());
            }
            return uids;
        }

        public void end() {
            if (!isEnding()) {
                JSONObject gameResults = new JSONObject("{}");
                startEnding();
                gameResults.put("player_scores", new JSONObject("{}"));
                if (teams > 1) {
                    gameResults.put("team_scores", new JSONObject("{}"));
                    for (Map.Entry<Team, Integer> entry : getTeamScores().entrySet()) {
                        gameResults.getJSONObject("team_scores").put(entry.getKey().name(), entry.getValue());
                        for (UUID uid : getPlayers(entry.getKey())) {
                            MysticPlayer mp = AccountManager.getMysticPlayer(uid);
                            mp.putData("points", mp.getInt("points") + entry.getValue());
                        }
                    }
                }
                for (Map.Entry<UUID, GamePlayer> entry : players.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    player.setGameMode(GameMode.SPECTATOR);
                    getPlayer(player.getUniqueId()).setTeam(Team.SPECTATOR);
                    gameResults.getJSONObject("player_scores").put(entry.getKey().toString(), getPlayerScores().get(entry.getKey()));
                    MysticPlayer mp = AccountManager.getMysticPlayer(entry.getKey());
                    mp.putData("points", mp.getInt("points") + getGameState().getScore(player));
                }

                gameResults.put("duration", getCurrentDuration());
                gameResults.put("game_specific", controller.end());
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

                    noBuildZones.clear();
                    Game.this.close();
                }, 15 * 20);


            }

        }

        public void addPlayer(UUID uid) {
            Player player = Bukkit.getPlayer(uid);
            if (player.getWorld().hasMetadata("game")) {
                player.sendMessage(MessageUtils.prefixes("game") + "You can't join a game while you're already in one!");
                return;
            }
            if (players.size() >= maxPlayers || !gameState.acceptingPlayers()) {
                player.setGameMode(GameMode.SPECTATOR);
                UUID[] uids = getPlayers().keySet().toArray(new UUID[getPlayers().keySet().size()]);
                if(!scoreboards.containsKey(uid)) scoreboards.put(uid, new ScoreboardManager(player));
                scoreboards.get(uid).set();
                GamePlayer gamePlayer = new GamePlayer(uid);
                gamePlayer.setTeam(Team.SPECTATOR);
                players.put(player.getUniqueId(), gamePlayer);
                player.teleport(Bukkit.getPlayer(uids[(new Random().nextInt(uids.length))]));
                player.sendMessage(MessageUtils.colorize("&7You are now spectating this game."));
                return;
            }
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

                players.put(player.getUniqueId(), new GamePlayer(player.getUniqueId()));
                player.setGameMode(GameMode.SPECTATOR);
                sendMessage("&3" + player.getName() + "&e has joined! (&3" + players.size() + "&e/&3" + maxPlayers + "&e)");
                if (players.size() >= minPlayers && !gameState.countdown()) {
                    gameState.startCountdown();

                }
            }), 0);


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

        public Map<UUID, GamePlayer> getPlayers() {
            return players;
        }

        public GamePlayer getPlayer(UUID uid) {
            return players.get(uid);
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

        public void kill(Player player, EntityDamageEvent.DamageCause cause) {
            GamePlayer gamePlayer = getPlayers().get(player.getUniqueId());
            gamePlayer.setLives(gamePlayer.getLives() - 1);
            player.setHealth(player.getHealthScale());
            player.setFoodLevel(20);
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
                }, ()->{}), 0);
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


        public void spawnPlayer(Player player) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            GamePlayer gamePlayer = getPlayer(player.getUniqueId());
            player.setHealth(player.getHealthScale());
            List<Arena.Spawn> spawns = arena.getSpawns(gamePlayer.getTeam());
            player.teleport(spawns.get(new Random().nextInt(spawns.size())).getLocation());
        }

        public void processDamage(Player victim, double damage, EntityDamageEvent.DamageCause cause) {

            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                Entity perp = null;
                if (victim.hasMetadata("last_damager")) {
                    perp = Bukkit.getEntity((UUID) victim.getMetadata("last_damager").get(0).value());
                    if (perp == null) return;
                    if (perp instanceof Player) {
                        Player perp1 = (Player) perp;
                        if (perp1.equals(victim) || (!isFriendlyFire() && getPlayer(victim.getUniqueId()).getTeam().equals(getPlayer(perp1.getUniqueId()).getTeam())))
                            return;

                    }
                }
                if (victim.getHealth() - damage <= 0) {
                    kill(victim, cause);
                    return;
                }
                victim.setMetadata("do_damage", new FixedMetadataValue(Utils.getPlugin(), damage));
                victim.damage(damage, perp);
            }, 1);
        }

        public void reset() {
            lobbyOpen = true;
            gameRunning = false;
            countdown = false;
            STARTED = 0;
            ending = false;
        }

        public GameState clone() {
            GameState state = new GameState();
            state.playerScores = playerScores;
            state.teamScores = teamScores;
            state.players = players;

            state.lobbyOpen = lobbyOpen;
            state.gameRunning = gameRunning;
            state.countdown = countdown;
            state.ending = ending;
            state.STARTED = STARTED;

            return state;
        }

        public long getCurrentDuration() {
            return new Date().getTime() - getStarted();
        }

        public long getStarted() {
            return STARTED;
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
        Runnable reset;
        TimerTick tick;
        boolean cancel = false;

        CountdownRunnable(int timer, TimerTick tick, Runnable finish, Runnable reset) {
            this.timer = timer;
            this.date = new Date().getTime();
            this.finish = finish;
            this.tick = tick;
            this.reset = reset;
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
            else reset.run();
        }
    }

    private interface TimerTick {
        public abstract boolean go(int timer);
    }

    public interface GameController {

        public void start();

        public boolean check();

        public JSONObject end();

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
            Game.this.scoreboards.get(uid).getScoreboard().getTeam(team.name()).addEntry(Bukkit.getPlayer(uid).getName());

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