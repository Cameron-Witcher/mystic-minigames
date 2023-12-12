package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.core.utils.sql.SQLUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.games.arenas.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.json2.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        ArenaManager.registerArenas();
        GameManager.init();

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
        Map<UUID, Scoreboard> scoreboards = new HashMap<>();


    }
}
