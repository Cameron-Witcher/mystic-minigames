package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.utils.games.arenas.ArenaManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.json2.JSONObject;

public class Utils {

    private static MysticMinigames plugin;

    public static void init(MysticMinigames mainClass) {
        plugin = mainClass;
        PlaceholderUtils.registerPlaceholder("team", (player) -> {
            if (!player.getWorld().hasMetadata("game")) return "NO-GAME";
            return ((Game) player.getWorld().getMetadata("game").get(0).value()).getPlayer(player.getUniqueId()).getTeam().name();
        });
        ArenaManager.registerArenas();
        GameManager.init();

        CoreUtils.addPalpitation(() -> {
            for (Game game : GameManager.getGames().values()) {
                if (game.getController().check()) game.end();
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
}
