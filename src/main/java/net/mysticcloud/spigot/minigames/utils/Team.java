package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.*;

public enum Team {
    RED(ChatColor.RED), BLUE(ChatColor.BLUE), GREEN(ChatColor.GREEN), YELLOW(ChatColor.YELLOW), NONE(ChatColor.RESET), SPECTATOR(ChatColor.RESET);

    final ChatColor chatColor;

    Team(ChatColor chatColor) {
        this.chatColor = chatColor;
    }

    public static List<Team> getTeamsFromMax(int maxTeams) {
        List<Team> teams = new ArrayList<>();
        int i = 0;
        for (Team team : values()) {
            if (i >= maxTeams)
                break;
            teams.add(team);
            i = i + 1;
        }
        return teams;
    }

    public static void sort(Map<UUID, Team> players, int teams) {
        Set<UUID> pls = players.keySet();
        Team[] teamArray = getTeamsFromMax(teams).toArray(new Team[teams]);
        int i = 0;
        for (UUID uid : pls) {
            players.put(uid, teamArray[i]);
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize("&eYou're on the " + teamArray[i].chatColor + "&l" + teamArray[i].name() + "&r&e team!"));
            i = i + 1;
            if (i >= teams)
                i = 0;

        }
    }

    public ChatColor chatColor() {
        return chatColor;
    }
}
