package net.mysticcloud.spigot.minigames.utils.misc;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.PlaceholderUtils;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ScoreboardManager {

    Scoreboard board;
    UUID uid;
    List<String> lines = new ArrayList<>();

    public ScoreboardManager(Player player) {
        board = Bukkit.getScoreboardManager().getNewScoreboard();
        uid = player.getUniqueId();
        for (net.mysticcloud.spigot.minigames.utils.Team team : net.mysticcloud.spigot.minigames.utils.Team.values())
            board.registerNewTeam(team.name()).setColor(team.chatColor());
    }

    public void belowName(){

    }

    public void sidebar(String title, List<String> lines) {
        this.lines = lines;

        Player player = Bukkit.getPlayer(uid);

        Objective obj = board.registerNewObjective("sidebar", Criteria.DUMMY.getName(), MessageUtils.colorize(PlaceholderUtils.markup(player, PlaceholderUtils.replace(player, title))));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        List<String> usedColors = new ArrayList<>();
        int i = lines.size() + 1;
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
            team.setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(Bukkit.getPlayer(uid), s)));
            obj.getScore(color).setScore(i);
            i = i - 1;
        }


    }


    public void update() {

        int i = lines.size() + 1;
        for (String s : lines) {
            board.getTeam(i + "").setPrefix(MessageUtils.colorize(PlaceholderUtils.replace(Bukkit.getPlayer(uid), s)));
            i = i - 1;
        }
    }

    public void set() {
        Bukkit.getPlayer(uid).setScoreboard(board);
    }

    public Scoreboard getScoreboard() {
        return board;
    }
}