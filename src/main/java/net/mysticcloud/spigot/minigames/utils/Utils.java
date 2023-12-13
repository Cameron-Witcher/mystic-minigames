package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.accounts.AccountManager;
import net.mysticcloud.spigot.core.utils.gui.GuiInventory;
import net.mysticcloud.spigot.core.utils.gui.GuiItem;
import net.mysticcloud.spigot.core.utils.gui.GuiManager;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.core.utils.sql.SQLUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.games.arenas.ArenaManager;
import net.mysticcloud.spigot.minigames.utils.misc.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json2.JSONObject;

import java.util.*;

public class Utils {

    private static MysticMinigames plugin;
    private static Map<UUID, ScoreboardManager> scoreboards = new HashMap<>();

    public static void init(MysticMinigames mainClass) {
        plugin = mainClass;

        registerPlaceholders();
        ArenaManager.registerArenas();
        GameManager.init();
        SQLUtils.createDatabase("results", SQLUtils.SQLDriver.MYSQL, "sql.vanillaflux.com", "minigame_results", 3306, "mystic", "9ah#G@RjPc@@Riki");
        registerPalpitations();
    }

    private static void registerPalpitations() {
        CoreUtils.addPalpitation(() -> {
            for (World world : Bukkit.getWorlds()) {
                for (Player player : world.getPlayers()) {
                    if (world.hasMetadata("game")) {
                        Game game = (Game) world.getMetadata("game").get(0).value();
                        game.getScoreboardManager(player.getUniqueId()).update();
                    } else Utils.getScoreboardManager(player.getUniqueId()).update();
                }

            }

            for (Game game : GameManager.getGames().values()) {
                if (game.getController().check()) game.getGameState().end();
            }
        });
    }

    private static void registerPlaceholders() {
        PlaceholderUtils.registerPlaceholder("team", (player) -> {
            if (!player.getWorld().hasMetadata("game")) return "NO-GAME";
            return ((Game) player.getWorld().getMetadata("game").get(0).value()).getGameState().getPlayer(player.getUniqueId()).getTeam().name();
        });
        PlaceholderUtils.registerPlaceholder("points", (player) -> {
            return AccountManager.getMysticPlayer(player.getUniqueId()).getInt("points") + "";
        });
        for (net.mysticcloud.spigot.minigames.utils.Team team : net.mysticcloud.spigot.minigames.utils.Team.values()) {
            PlaceholderUtils.registerPlaceholder("team_" + team.name() + "_score", (player) -> {
                if (!player.getWorld().hasMetadata("game")) return "NO-GAME";
                Game game = (Game) player.getWorld().getMetadata("game").get(0).value();
                return game.getGameState().getScore(team) + "";
            });
        }
        PlaceholderUtils.registerPlaceholder("score", (player -> {
            if (!player.getWorld().hasMetadata("game")) return "NO-GAME";
            Game game = (Game) player.getWorld().getMetadata("game").get(0).value();
            return game.getGameState().getScore(player) + "";
        }));
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

    public static ScoreboardManager getScoreboardManager(UUID uid) {
        if (scoreboards.containsKey(uid)) return scoreboards.get(uid);
        ScoreboardManager manager = new ScoreboardManager(Bukkit.getPlayer(uid));
        manager.sidebar("&3&lMYSTIC&7&lCLOUD", Arrays.asList(new String[]{"&1", "&a&lPLAYER&8:", "&7 Points&8: &a%points%" + Symbols.STAR_1, "&2", "&c&lSERVER&8:", "&7 Online: &c%online%", "&3"}));
        scoreboards.put(uid, manager);
        return manager;
    }


}

