package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.minigames.utils.games.CTW;
import net.mysticcloud.spigot.minigames.utils.games.Dodgeball;
import net.mysticcloud.spigot.minigames.utils.games.HotPotato;
import net.mysticcloud.spigot.minigames.utils.games.OITQ;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.games.arenas.ArenaManager;
import org.bukkit.ChatColor;
import org.json2.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GameManager {

    private static final File gameDir = new File(Utils.getPlugin().getDataFolder() + "/games");

    private static final Map<String, Game> games = new HashMap<>();

    public static void init() {
        MessageUtils.prefixes("game", "&3&lGames &7> &f");
        if (!gameDir.exists()) {
            MessageUtils.log("Region file creation: " + (gameDir.mkdirs() ? "success" : "FAILED."));
        }

        for (File file : gameDir.listFiles()) {

            JSONObject data;
            try {
                Scanner reader = new Scanner(file);
                data = new JSONObject(reader.nextLine());
                reader.close();
            } catch (FileNotFoundException e) {
                System.out.println("An error occurred.");
                throw new RuntimeException(e);
            }
            Game game = createGame(data.getString("game"), ArenaManager.getArena(data.getString("arena")), data.has("teams") ? data.getInt("teams") : -1, data);

        }
    }


    public static void saveGame(Game game) {

        File file = new File(gameDir.getPath() + "/" + game.getName() + "-" + game.getArena().getName() + ".game");
        if (file.exists()) {
            file.delete();
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JSONObject obj = game.toJson();
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(obj.toString());
            writer.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            throw new RuntimeException(e);

        }

        try {
            game.getArena().saveToFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    public static Map<String, Game> getGames() {
        return games;
    }

    public static Game createGame(String gameName, Arena arena, int teams) {
        return createGame(gameName, arena, teams, null);
    }

    public static Game createGame(String gameName, Arena arena, int teams, JSONObject data) {
        final Game game;
        if (data == null) data = new JSONObject("{}");
        switch (gameName.toLowerCase()) {
            case "ctw":
                game = new CTW(arena, teams);
                break;
            case "oitq":
                game = new OITQ(arena);
                break;
            case "hotpotato":
                game = new HotPotato(arena);
                break;
            case "dodgeball":
                game = new Dodgeball(arena, teams);
                break;
            default:
                game = null;
                break;
        }
        String key = game.getId();
        games.put(key, game);
        PlaceholderUtils.registerPlaceholder("game_info_players_" + key, (player) -> {
            return game.getGameState().getPlayers().size() + "";
        });
        PlaceholderUtils.registerPlaceholder("game_info_max_players_" + key, (player) -> {
            return game.getMAX_PLAYERS() + "";
        });
        PlaceholderUtils.registerPlaceholder("game_info_status_" + key, (player) -> {
            return game.getGameState().gameRunning ? ChatColor.RED + "Running" : (game.getGameState().countdown ? ChatColor.YELLOW + "Starting" : (game.getGameState().lobbyOpen ? ChatColor.GREEN + "Ready" : ChatColor.DARK_RED + "Error"));
        });
        saveGame(game);
        return game;
    }

    public static Game getGame(String name) {
        return games.get(name);
    }


    public static void removeGame(String name) {
        games.remove(name);
    }
}
