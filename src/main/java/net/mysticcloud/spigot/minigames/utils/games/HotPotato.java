package net.mysticcloud.spigot.minigames.utils.games;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
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

    private Objective scoresObjective = getScoreboardManager().getScoreboard().registerNewObjective("lives", "dummy", "Lives");


    public HotPotato(Arena arena) {
        super("HotPotato", arena);
        setTeams(1);
        setMinPlayers(2);
        setMaxPlayers(20);

        setFriendlyFire(true);

        scoresObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        scoresObjective.setDisplayName(ChatColor.GREEN + Symbols.STAR_1.toString());

        setController(new GameController() {

            long LASTED = 0;
            long SCORE_CHECK = 0;
            long NOW = 0;
            boolean CHECK_SCORE = false;

            @Override
            public void start() {
                for (UUID uid : getPlayers().keySet()) {
                    spawnPlayer(Bukkit.getPlayer(uid));
                }
                UUID[] uids = getPlayers().keySet().toArray(new UUID[getPlayers().keySet().size()]);
                setHolder(Bukkit.getPlayer(uids[(new Random().nextInt(uids.length))]));
                STARTED = new Date().getTime();

            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                NOW = new Date().getTime();
                LASTED = NOW - STARTED;
                if (NOW - SCORE_CHECK >= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS)) {
                    CHECK_SCORE = true;
                    SCORE_CHECK = NOW;
                }
                for (GamePlayer gamePlayer : getPlayers().values()) {
                    Player player = Bukkit.getPlayer(gamePlayer.getUUID());
                    scoresObjective.getScore(player.getName()).setScore(getScore(player));
                    if (CHECK_SCORE && !potatoHolder.equals(gamePlayer.getUUID())) score(player);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.DARK_AQUA + MessageUtils.formatTimeRaw(DURATION - LASTED) + " | " + (getHolder().equals(gamePlayer.getUUID()) ? ChatColor.RED + "You have the potato!" : ChatColor.GREEN + "You don't have the potato!")));
                }

                CHECK_SCORE = false;
                return LASTED >= DURATION;
            }


            @Override
            public void end() {
                int z = getPlayerScores().size();
                for (Map.Entry<UUID, Integer> entry : sortPlayerScores().entrySet()) {
                    if (z == 1) {
                        sendMessage("&3" + Bukkit.getPlayer(entry.getKey()).getName() + "&7 came in 1st place!");
                    }
                    z = z - 1;
                    //Divvy rewards and send messages
                }
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
        setHolder(to);
    }

}