package net.mysticcloud.spigot.minigames.utils.misc;

import net.md_5.bungee.api.ChatColor;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.minigames.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.json2.JSONObject;

import java.util.*;

public class ScoreboardBuilder {

    Properties properties;

    public ScoreboardBuilder() {
        properties = new Properties();
    }

    public ScoreboardBuilder set(Object key, Object value) {
        properties.put(key, value);
        return this;
    }

    public CustomScoreboard build() {
        return new CustomScoreboard(properties);
    }


    public class CustomScoreboard {

        private final Map<UUID, Scoreboard> boards = new HashMap<>();
        private Properties properties;

        public CustomScoreboard(Properties properties) {
            this.properties = properties;
        }

        public void setTeam(Player player, net.mysticcloud.spigot.minigames.utils.Team team) {
            for (Scoreboard board : boards.values()) {
                board.getTeam(team.name()).addEntry(player.getName());
            }
        }


        public void updateObjective(String key, Player who, int score) {
            for (Scoreboard board : boards.values())
                board.getObjective(key).getScore(who.getName()).setScore(score);
        }

        public void createGlobalObjective(String key, DisplaySlot slot, String display){
            for(Map.Entry<UUID, Scoreboard> e : boards.entrySet()){
                e.getValue().registerNewObjective(key, Criteria.DUMMY, display);
                e.getValue().getObjective(key).setDisplaySlot(slot);

            }
        }

        public void removePlayer(UUID uid) {
            boards.remove(uid);
        }

        public void addPlayer(Player player) {
            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            for (net.mysticcloud.spigot.minigames.utils.Team team : net.mysticcloud.spigot.minigames.utils.Team.values())
                board.registerNewTeam(team.name()).setColor(team.chatColor());

            if (properties.containsKey("below_name")) {
                JSONObject json = (JSONObject) properties.get("below_name");
                createGlobalObjective(json.getString("key"), DisplaySlot.BELOW_NAME, MessageUtils.colorize(PlaceholderUtils.replace(player, json.getString("display"))));
                board.getObjective(json.getString("key")).getScore(player.getName()).setScore(0);
            }

            if (properties.containsKey("sidebar")) {
                JSONObject json = (JSONObject) properties.get("sidebar");
                Objective obj = board.registerNewObjective("sidebar", Criteria.DUMMY, MessageUtils.colorize(PlaceholderUtils.markup(player, PlaceholderUtils.replace(player, json.getString("title")))));
                obj.setDisplaySlot(DisplaySlot.SIDEBAR);

                String[] lines = (String[]) json.get("lines");

                List<String> usedColors = new ArrayList<>();
                int i = lines.length + 1;
                for (String s : lines) {
                    String color = "";
                    boolean go = true;
                    while (go) {
                        color = org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)] + "" + org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)];
                        if (!usedColors.contains(color)) {
                            usedColors.add(color);
                            go = false;
                        }
                    }
                    org.bukkit.scoreboard.Team team = board.registerNewTeam(i + "");
                    team.addEntry(color);
                    team.setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(player, s)));
                    obj.getScore(color).setScore(i);
                    i = i - 1;
                }
            }

            player.setScoreboard(board);
            boards.put(player.getUniqueId(), board);
        }


        public void update() {
            if (properties.containsKey("sidebar")) {
                JSONObject json = (JSONObject) properties.get("sidebar");
                String[] lines = (String[]) json.get("lines");
                for(Map.Entry<UUID, Scoreboard> e : boards.entrySet()){
                    int i = lines.length + 1;
                    for (String s : lines) {
                        e.getValue().getTeam(i + "").setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(Bukkit.getPlayer(e.getKey()), s)));
                        i = i - 1;
                    }
                }
            }

        }

        public void clearBoards(){
            boards.clear();
        }
    }
}
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//    //players
//    //info (lines and lives/scores)
////    List<String> lines = new ArrayList<>();
////    Map<UUID, Scoreboard> scoreboards = new HashMap<>();
//
//
//
//
//
//
//    public void sidebar(String title, List<String> lines) {
//        ScoreboardBuilder.this.lines = lines;
//
//        Objective obj = board.registerNewObjective("sidebar", Criteria.DUMMY, MessageUtils.colorize(PlaceholderUtils.markup(player, PlaceholderUtils.replace(player, title))));
//        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
//
//        List<String> usedColors = new ArrayList<>();
//        int i = lines.size() + 1;
//        for (String s : lines) {
//            String color = "";
//            boolean go = true;
//            while (go) {
//                color = org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)] + "" + org.bukkit.ChatColor.values()[new Random().nextInt(org.bukkit.ChatColor.values().length)];
//                if (!usedColors.contains(color)) {
//                    usedColors.add(color);
//                    go = false;
//                }
//            }
//            org.bukkit.scoreboard.Team team = board.registerNewTeam(i + "");
//            team.addEntry(color);
//            team.setPrefix(MessageUtils.colorize((s)));
//            obj.getScore(color).setScore(i);
//            i = i - 1;
//        }
//
//
//    }
//
//
//    class ScoreboardAdapter {
//
//        Scoreboard board;
//        UUID uid;
//
//
//        public ScoreboardAdapter(Player player) {
//            board = Bukkit.getScoreboardManager().getNewScoreboard();
//            uid = player.getUniqueId();
//            for (net.mysticcloud.spigot.minigames.utils.Team team : net.mysticcloud.spigot.minigames.utils.Team.values())
//                board.registerNewTeam(team.name()).setColor(team.chatColor());
//        }
//
//        public void belowName(){
//
//        }
//
//
//
//
//        public void update() {
//
//            int i = lines.size() + 1;
//            for (String s : lines) {
//                board.getTeam(i + "").setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(Bukkit.getPlayer(uid), s)));
//                i = i - 1;
//            }
//        }
//
//        public void set() {
//            Bukkit.getPlayer(uid).setScoreboard(board);
//        }
//
//        public Scoreboard getScoreboard() {
//            return board;
//        }
//    }
//}
