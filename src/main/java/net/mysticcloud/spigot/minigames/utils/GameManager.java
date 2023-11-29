package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.games.CTW;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Structure;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
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

    private static Map<String, Game> games = new HashMap<>();

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
            Game game = createGame(data.getString("game"), new Arena(data.getString("arena")), data.has("teams") ? data.getInt("teams") : -1, data);

        }
    }

    public static Game createGame(String gameName, Arena arena, int teams) {
        Game game = createGame(gameName, arena, teams, null);
        saveGame(game);
        return game;
    }

    public static void saveGame(Game game) {

        File file = new File(gameDir.getPath() + "/" + game.getName() + "-" + game.getArena().getName() + ".game");
        if (file.exists()) {
            file.delete();
            try {
                MessageUtils.log("Region file creation: " + (file.createNewFile() ? "success" : "FAILED."));
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


    }

    public static Map<String, Game> getGames() {
        return games;
    }

    public static Game createGame(String gameName, Arena arena, int teams, JSONObject data) {
        Game game;
        switch (gameName.toLowerCase()) {
            case "ctw":
                game = new CTW(new Arena(data.getString("arena")), data.getInt("teams"));
                break;
            default:
                game = null;
                break;
        }
        games.put(game.getName() + "-" + game.getArena().getName(), game);
        return new Game("", null);
    }

    public static Game getGame(String name) {
        return games.get(name);
    }


}
