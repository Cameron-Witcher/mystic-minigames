package net.mysticcloud.spigot.minigames.utils.games;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.misc.ScoreboardBuilder;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class HotPotato extends Game {

    private long STARTED;
    private final long DURATION = TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);

    private UUID potatoHolder = null;


    public HotPotato(Arena arena) {
        super("HotPotato", arena);
        setGameState(new HotPotatoGameState());
        setTEAMS(1);
        setMIN_PLAYERS(2);
        setMAX_PLAYERS(20);

        setFriendlyFire(true);

        JSONObject json = new JSONObject("{}");
        json.put("key", "score");
        json.put("display", "&c" + Symbols.STAR_1.toString());
        ScoreboardBuilder builder = new ScoreboardBuilder();
        builder.set("below_name", json);
        setCustomScoreboard(builder.build());


        setController(new GameController() {

            long LASTED = 0;
            long SCORE_CHECK = 0;
            long NOW = 0;
            boolean CHECK_SCORE = false;

            @Override
            public void start() {
                for (UUID uid : getGameState().getPlayers().keySet()) {
                    Player player = Bukkit.getPlayer(uid);
                    getGameState().spawnPlayer(player);
                    getCustomScoreboard().updateObjective("lives", player, 0);
                }


                STARTED = new Date().getTime();

            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                if (NOW == 0) {
                    //First run. Set delay
                    NOW = 1;
                    Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                        List<UUID> teamMembers = getGameState().getPlayers(Team.NONE);
                        setHolder(Bukkit.getPlayer(teamMembers.get(new Random().nextInt(teamMembers.size()))));
                        NOW = 2;
                    }, 5 * 20);
                }
                if (NOW == 1) {
                    return false;
                }
                NOW = new Date().getTime();
                LASTED = NOW - STARTED;
                if (NOW - SCORE_CHECK >= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)) {
                    CHECK_SCORE = true;
                    SCORE_CHECK = NOW;
                }
                for (GamePlayer gamePlayer : getGameState().getPlayers().values()) {
                    Player player = Bukkit.getPlayer(gamePlayer.getUUID());
                    if (CHECK_SCORE && !potatoHolder.equals(gamePlayer.getUUID())) getGameState().score(player);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.DARK_AQUA + MessageUtils.formatTimeRaw(DURATION - LASTED) + " | " + (getHolder().equals(gamePlayer.getUUID()) ? ChatColor.RED + "You have the potato!" : ChatColor.GREEN + "You don't have the potato!") + ChatColor.DARK_AQUA + " | " + getGameState().getScore(player) + " " + ChatColor.GREEN + Symbols.STAR_1));
                }

                CHECK_SCORE = false;
                return LASTED >= DURATION;
            }


            @Override
            public JSONObject end() {
                int z = getGameState().getPlayerScores().size();
                JSONObject extra = new JSONObject("{}");

                Map<Integer, UUID> placements = new HashMap<>();

                for (Map.Entry<UUID, Integer> entry : getGameState().sortPlayerScores().entrySet()) {
                    placements.put(z, entry.getKey());
                    z = z - 1;
                }

                sendMessage(MessageUtils.colorize("&7--------------------------"));
                sendMessage("");
                sendMessage("");
                if (placements.isEmpty()) {
                    sendMessage(ChatColor.RED + "There was a draw.");
                } else {
                    if (placements.containsKey(1))
                        sendMessage("  " + "&b" + Bukkit.getPlayer(placements.get(1)).getName() + "&8 came in 1st place!");
                    if (placements.containsKey(2))
                        sendMessage("  " + "&6" + Bukkit.getPlayer(placements.get(2)).getName() + "&8 came in 2nd place!");
                    if (placements.containsKey(3))
                        sendMessage("  " + "&7" + Bukkit.getPlayer(placements.get(3)).getName() + "&8 came in 3rd place!");
                }
                sendMessage("");
                sendMessage("");
                sendMessage(MessageUtils.colorize("&7--------------------------"));

                return extra;
            }

            @Override
            public void generate() {
                JSONArray spawns = arena.getData().getJSONArray("spawns");
                for (int i = 0; i < spawns.length(); i++) {
                    JSONObject spawnData = spawns.getJSONObject(i);
                    Location loc = Utils.decryptLocation(arena.getWorld(), spawnData.getJSONObject("location"));
                    addNoBuildZone(loc);
                }
            }
        });
    }


    private void setHolder(Player player) {
        potatoHolder = player.getUniqueId();
        player.sendMessage(MessageUtils.colorize("&cYou have the Potato!"));
    }

    public UUID getHolder() {
        return potatoHolder;
    }

    public void swapHolder(Player from, Player to) {
        from.sendMessage(MessageUtils.colorize("&aYou have passed the Potato to " + to.getName()));
        from.playSound(from, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        to.playSound(to, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        to.setVelocity(from.getVelocity().setY(0.3));
        setHolder(to);
    }

    public class HotPotatoGameState extends GameState {

        @Override
        public void processDamage(Player victim, double damage, EntityDamageEvent.DamageCause cause) {
            Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                if (victim.hasMetadata("last_damager")) {
                    Entity perp = Bukkit.getEntity((UUID) victim.getMetadata("last_damager").get(0).value());
                    if (getHolder().equals(perp.getUniqueId())) swapHolder(((Player) perp), victim);
                }
                super.processDamage(victim, 0, cause);
            }, 0);

        }

        @Override
        public void removePlayer(UUID uid, boolean list) {
            if (getHolder() != null && getHolder().equals(uid)) {
                List<UUID> teamMembers = getPlayers(Team.NONE);
                if (!teamMembers.isEmpty()) {
                    UUID uid2 = teamMembers.get(new Random().nextInt(teamMembers.size()));
                    while (uid2.equals(uid))
                        uid2 = teamMembers.get(new Random().nextInt(teamMembers.size()));
                    setHolder(Bukkit.getPlayer(uid2));
                }
            }
            super.removePlayer(uid, list);
        }


        @Override
        public int score(Player player, int amount) {
            super.score(player, amount);
            int score = getScore(player);
            getCustomScoreboard().updateObjective("score", player, score);
            return score;
        }
    }

}