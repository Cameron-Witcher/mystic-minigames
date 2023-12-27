package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.accounts.AccountManager;
import net.mysticcloud.spigot.core.utils.accounts.MysticPlayer;
import net.mysticcloud.spigot.core.utils.gui.GuiInventory;
import net.mysticcloud.spigot.core.utils.gui.GuiManager;
import net.mysticcloud.spigot.core.utils.npc.Npc;
import net.mysticcloud.spigot.core.utils.npc.NpcManager;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;

import net.mysticcloud.spigot.minigames.utils.misc.ScoreboardBuilder;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
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
    private ScoreboardBuilder.CustomScoreboard customScoreboard = new ScoreboardBuilder().build();
    private final Map<UUID, ItemStack[]> inventoryList = new HashMap<>();
    private int TEAMS = 0, MIN_PLAYERS = 2, MAX_PLAYERS = 10;
    private GameController controller = null;
    private boolean generated = false;
    private boolean friendlyFire = true;
    protected GuiInventory shop = null;
    private final List<Npc> npcs = new ArrayList<>();


    public Game(String gameName, Arena arena) {
        this.gameName = gameName;
        this.arena = arena;
        arena.setGame(this);
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public String getId() {
        return gameName + "-" + arena.getName();
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

    protected void setTEAMS(int i) {
        TEAMS = i;
    }

    protected void setMIN_PLAYERS(int i) {
        MIN_PLAYERS = i;
    }

    protected void setMAX_PLAYERS(int i) {
        MAX_PLAYERS = i;
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
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize(PlaceholderUtils.replace(Bukkit.getPlayer(uid), message)));
    }

    public void addNpc(Npc npc) {
        npcs.add(npc);
    }

    public void close() {
        generated = false;
        arena.regenerate();
        generated = true;
    }

    public int getTEAMS() {
        return TEAMS;
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
        if (TEAMS > 1) json.put("teams", TEAMS);
        json.put("extra", data);
        return json;
    }

    public GameController getController() {
        return controller;
    }


    public void defaultDeathMessages(Player player, EntityDamageEvent.DamageCause cause) {
        GamePlayer gamePlayer = gameState.getPlayer(player.getUniqueId());
        Entity entity = player.hasMetadata("last_damager") ? (Entity) (player.getMetadata("last_damager").get(0).value()) : null;

        String victim = (gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName();
        String action = " was killed";
        String ending = "!";
        switch (cause) {
            case FALL:
                action = " fell to their death";
                ending = ".";
                if (entity != null) {
                    action = " was pushed off a cliff by ";
                    ending = gameState.getPlayers().containsKey(entity.getUniqueId()) ? (gameState.getPlayer(entity.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : gameState.getPlayer(entity.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e." : "&3" + entity.getName() + "&e.";
                }
                break;
            case ENTITY_EXPLOSION:
            case BLOCK_EXPLOSION:
                action = " blew up";
                ending = "!";
                if (entity != null && entity.hasMetadata("placer")) {
                    Entity placer = (Entity) entity.getMetadata("placer").get(0).value();
                    action = " was blown up";
                    ending = " by " + (gameState.getPlayer(placer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : gameState.getPlayer(placer.getUniqueId()).getTeam().chatColor()) + placer.getName() + "&e.";

                }
                break;
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
        rocket.setMetadata("game_rocket", new FixedMetadataValue(Utils.getPlugin(), this));
        return rocket;
    }

    public int getMAX_PLAYERS() {
        return MAX_PLAYERS;
    }

    public void openShop(Player player) {
        if (shop != null) GuiManager.openGui(player, shop);


    }

    public void explodeBlocks(List<Block> blocks) {
        for (Block block : blocks)
            block.setType(Material.AIR);

    }

    public ScoreboardBuilder.CustomScoreboard getCustomScoreboard() {
        return customScoreboard;
    }

    public void setCustomScoreboard(ScoreboardBuilder.CustomScoreboard customScoreboard) {
        this.customScoreboard = customScoreboard;
    }


    public class GameState implements Cloneable {

        private final Map<UUID, Integer> playerScores = new HashMap<>();
        private final Map<Team, Integer> teamScores = new HashMap<>();
        private final Map<UUID, GamePlayer> players = new HashMap<>();

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
                Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new CountdownRunnable(20, (int t) -> {
                    if (getPlayers().size() < MIN_PLAYERS) {
                        sendMessage(MessageUtils.colorize("&cNot enough players. Cancelling countdown."));
                        return true;
                    }
                    if (t == 20 || t == 10 || t <= 5) {
                        sendMessage(MessageUtils.colorize("&3Starting in " + t + " second" + (t == 1 ? "" : "s") + "!"));
                        if (t <= 3) for (UUID uid : getPlayers().keySet()) {
                            Player player = Bukkit.getPlayer(uid);
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

                }, () -> {
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
            players.get(uid).setDisplayName(ChatColor.RESET + "", ChatColor.RESET + "");
            if (list) players.remove(uid);
            Player player = Bukkit.getPlayer(uid);
            player.setFallDistance(0);
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            player.setFoodLevel(20);
            player.setGameMode(GameMode.SURVIVAL);
            if (inventoryList.containsKey(player.getUniqueId()))
                player.getInventory().setContents(inventoryList.get(player.getUniqueId()));
            customScoreboard.removePlayer(uid);
            if (Bukkit.getPlayer(uid) != null)
                Utils.getCustomScoreboard().addPlayer(Bukkit.getPlayer(uid));
            inventoryList.remove(player.getUniqueId());
            player.removeMetadata("original_team", Utils.getPlugin());
            player.setDisplayName(ChatColor.RESET + player.getName());
        }

        public List<UUID> getPlayers(Team team) {
            List<UUID> uids = new ArrayList<>();
            for (GamePlayer gamePlayer : gameState.getPlayers().values()) {
                if (gamePlayer.getTeam().equals(team)) uids.add(gamePlayer.getUUID());
            }
            return uids;
        }

        public void end() {
            customScoreboard.clearBoards();
            if (!isEnding()) {
                JSONObject gameResults = new JSONObject("{}");
                startEnding();
                gameResults.put("player_scores", new JSONObject("{}"));
                if (TEAMS > 1) {
                    gameResults.put("team_scores", new JSONObject("{}"));
                    for (Map.Entry<Team, Integer> entry : getTeamScores().entrySet()) {
                        gameResults.getJSONObject("team_scores").put(entry.getKey().name(), entry.getValue());
                    }
                }
                for (Map.Entry<UUID, GamePlayer> entry : players.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    player.setGameMode(GameMode.SPECTATOR);
                    getPlayer(player.getUniqueId()).setTeam(Team.SPECTATOR);
                    gameResults.getJSONObject("player_scores").put(entry.getKey().toString(), getPlayerScores().get(entry.getKey()));
                    MysticPlayer mp = AccountManager.getMysticPlayer(entry.getKey());
                    mp.putData("points", mp.getInt("points") + getGameState().getScore(player));
                    if (TEAMS > 1) {
                        Team team = (Team) player.getMetadata("original_team").get(0).value();
                        mp.putData("points", mp.getInt("points") + getGameState().getScore(team));
                    }
                }

                gameResults.put("duration", getCurrentDuration());
                gameResults.put("game_specific", controller.end());
                Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {

                    for (UUID uid : players.keySet())
                        removePlayer(uid, false);

                    for (Npc npc : npcs)
                        NpcManager.removeNpc(npc.getUid());

                    npcs.clear();

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
            customScoreboard.addPlayer(player);
            if (players.size() >= MAX_PLAYERS || !gameState.acceptingPlayers()) {
                player.setGameMode(GameMode.SPECTATOR);
                UUID[] uids = getPlayers().keySet().toArray(new UUID[getPlayers().keySet().size()]);

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
                sendMessage("&3" + player.getName() + "&e has joined! (&3" + players.size() + "&e/&3" + MAX_PLAYERS + "&e)");
                if (players.size() >= MIN_PLAYERS && !gameState.countdown()) {
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
            player.removeMetadata("do_damage", Utils.getPlugin());
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
                }, () -> {
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


        public void spawnPlayer(Player player) {
            player.getInventory().clear();
            player.setGameMode(GameMode.SURVIVAL);
            GamePlayer gamePlayer = getPlayer(player.getUniqueId());
            player.setHealth(player.getHealthScale());
            player.setFoodLevel(20);
            List<Arena.Spawn> spawns = arena.getSpawns(gamePlayer.getTeam());
            Location spawn = spawns.get(new Random().nextInt(spawns.size())).getLocation();
            int i = 0;
            while (!checkSpawn(spawn)) {
                if (i >= 5) break;
                spawn = spawns.get(new Random().nextInt(spawns.size())).getLocation();
                i = i + 1;
            }
            player.teleport(spawn);
        }

        private boolean checkSpawn(Location location) {
            for (GamePlayer gamePlayer : getPlayers().values()) {
                if (CoreUtils.distance(Bukkit.getPlayer(gamePlayer.getUUID()).getLocation(), location) < 2)
                    return false;
            }
            return true;
        }

        public void processDamage(Player victim, double damage, EntityDamageEvent.DamageCause cause) {

            Bukkit.getScheduler().runTaskLaterAsynchronously(Utils.getPlugin(), () -> {
                try {
                    Entity perp = ((Entity) (victim.getMetadata("last_damager").get(0).value())).hasMetadata("placer") ? (Entity) ((Entity) victim.getMetadata("last_damager").get(0).value()).getMetadata("placer").get(0).value() : (Entity) victim.getMetadata("last_damager").get(0).value();
                    if (perp instanceof Player) {
                        Player perp1 = (Player) perp;
                        if (perp1.equals(victim) || (!isFriendlyFire() && getPlayer(victim.getUniqueId()).getTeam().equals(getPlayer(perp1.getUniqueId()).getTeam())))
                            return;

                    }
                    if (perp instanceof Firework) return;
                    victim.setMetadata("do_damage", new FixedMetadataValue(Utils.getPlugin(), damage));
                    if (victim.getHealth() - damage > 0 || ((victim.getHealth() - damage <= 0 && (victim.getInventory().getItemInOffHand().getType().equals(Material.TOTEM_OF_UNDYING) || victim.getInventory().getItemInMainHand().getType().equals(Material.TOTEM_OF_UNDYING)))))
                        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                            victim.damage(damage, ((Entity) (victim.getMetadata("last_damager").get(0).value())));

                        }, 0);
                } catch (IndexOutOfBoundsException ex) {
                    victim.setMetadata("do_damage", new FixedMetadataValue(Utils.getPlugin(), damage));
                    if (victim.getHealth() - damage > 0 || ((victim.getHealth() - damage <= 0 && (victim.getInventory().getItemInOffHand().getType().equals(Material.TOTEM_OF_UNDYING) || victim.getInventory().getItemInMainHand().getType().equals(Material.TOTEM_OF_UNDYING)))))
                        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                            victim.damage(damage);
                        }, 0);
                }
                if ((victim.getHealth() - damage <= 0 && (!victim.getInventory().getItemInOffHand().getType().equals(Material.TOTEM_OF_UNDYING) && !victim.getInventory().getItemInMainHand().getType().equals(Material.TOTEM_OF_UNDYING))) || cause.equals(EntityDamageEvent.DamageCause.VOID)) {
                    Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                        kill(victim, cause);
                    }, 0);
                    return;
                }
            }, 0);
        }

        public void reset() {
            lobbyOpen = true;
            gameRunning = false;
            countdown = false;
            STARTED = 0;
            ending = false;
        }


        public long getCurrentDuration() {
            return new Date().getTime() - getStarted();
        }

        public long getStarted() {
            return STARTED;
        }

        @Override
        public GameState clone() {
            try {
                GameState clone = (GameState) super.clone();
                // TODO: copy mutable state here, so the clone can't change the internals of the original
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
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
            getCustomScoreboard().setTeam(Bukkit.getPlayer(uid), team);
//            Game.this.getScoreboardManager(uid).getScoreboard().getTeam(team.name()).addEntry(Bukkit.getPlayer(uid).getName());
            setDisplayName(team.chatColor() + "[" + team.name() + "] ", ChatColor.RESET + "");
        }

        public void setDisplayName(String prefix, String suffix) {
            if (Bukkit.getPlayer(uid) != null) {
                Player player = Bukkit.getPlayer(uid);
                assert player != null;
                player.setPlayerListName(MessageUtils.colorize(prefix) + player.getName() + MessageUtils.colorize(suffix));
                player.setCustomName(MessageUtils.colorize(prefix) + player.getName() + MessageUtils.colorize(suffix));
                player.setDisplayName(MessageUtils.colorize(prefix) + player.getName() + MessageUtils.colorize(suffix));
            }
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