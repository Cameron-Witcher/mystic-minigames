package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.accounts.AccountManager;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.core.utils.sql.SQLUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.games.arenas.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import org.bukkit.scoreboard.Team;
import org.json2.JSONObject;

import java.util.*;

public class Utils {

    private static MysticMinigames plugin;

    private static Scoreboard scoreboard;

    public static void init(MysticMinigames mainClass) {
        plugin = mainClass;
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        PlaceholderUtils.registerPlaceholder("team", (player) -> {
            if (!player.getWorld().hasMetadata("game")) return "NO-GAME";
            return ((Game) player.getWorld().getMetadata("game").get(0).value()).getGameState().getPlayer(player.getUniqueId()).getTeam().name();
        });
        PlaceholderUtils.registerPlaceholder("points",(player)->{
            return AccountManager.getMysticPlayer(player.getUniqueId()).getInt("points") + "";
        });
        ArenaManager.registerArenas();
        GameManager.init();
        ScoreboardManager.init();

        SQLUtils.createDatabase("results", SQLUtils.SQLDriver.MYSQL, "sql.vanillaflux.com", "minigame_results", 3306, "mystic", "9ah#G@RjPc@@Riki");

        CoreUtils.addPalpitation(() -> {
            for (Game game : GameManager.getGames().values()) {
                if (game.getController().check()) game.getGameState().end();
            }
        });
    }

    public static MysticMinigames getPlugin() {
        return plugin;
    }

    public static Location decryptLocation(World world, JSONObject location) {
        return new Location(world, location.getDouble("x"), location.getDouble("y"), location.getDouble("z"), location.getFloat("yaw"), location.getFloat("pitch"));
    }

    public static JSONObject encryptLocation(Location location) {
        JSONObject json = new JSONObject("{}");
        json.put("x", location.getX());
        json.put("y", location.getY());
        json.put("z", location.getZ());
        json.put("yaw", location.getYaw());
        json.put("pitch", location.getPitch());
        return json;

    }

    public static class ScoreboardManager {

        static String TITLE = "&3&lMYSTIC&7&lCLOUD";
        static Map<UUID, Scoreboard> scoreboards = new HashMap<>();
        static LinkedList<String> LINES = new LinkedList<>();

        public static void init(){
            LINES.add("&1");
            LINES.add("&a&lPLAYER&8:");
            LINES.add("&7 Points&8: &a%points%");
            LINES.add("&2");
            LINES.add("&c&lSERVER&8:");
            LINES.add("&7 Online&8: &c%online%");
        }

        public static Scoreboard getScoreboard(Player pl){
            if(scoreboards.containsKey(pl.getUniqueId())) return scoreboards.get(pl.getUniqueId());
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

            Objective obj = board.registerNewObjective("title", Criteria.DUMMY.getName(),
                    MessageUtils.colorize(PlaceholderUtils.markup(pl, PlaceholderUtils.replace(pl, TITLE))));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);

            List<String> usedColors = new ArrayList<>();
            int i = LINES.size() + 1;
            for (String s : LINES) {
                String color = "";
                boolean go = true;
                while (go) {
                    color = org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)] + ""
                            + org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)];
                    if (!usedColors.contains(color)) {
                        usedColors.add(color);
                        go = false;
                    }
                }
                Team team = board.registerNewTeam(i + "");
                team.addEntry(color);
                team.setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(pl, s)));
                obj.getScore(color).setScore(i);
                i = i - 1;

            }

            scoreboards.put(pl.getUniqueId(), board);
            return board;
        }



        public static void updateScoreboard(Player pl) {
            if (scoreboards.containsKey(pl.getUniqueId())) {
                Scoreboard board = scoreboards.get(pl.getUniqueId());
                int i = LINES.size() + 1;
                for (String s : LINES) {
                    board.getTeam(i + "").setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(pl, s)));
                    i = i - 1;
                }
            }
        }


    }
}
