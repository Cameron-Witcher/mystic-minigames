package net.mysticcloud.spigot.minigames.utils;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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

    public static Map<UUID, Team> sort(Collection<UUID> uids, int teams, Game game) {
        Team[] teamArray = getTeamsFromMax(teams).toArray(new Team[teams]);
        Map<UUID, Team> players = new HashMap<>();
        int i = 0;
        for (UUID uid : uids) {
            players.put(uid, teamArray[i]);
            Bukkit.getPlayer(uid).sendMessage(MessageUtils.colorize("&eYou're on the " + teamArray[i].chatColor + "&l" + teamArray[i].name() + "&r&e team!"));
            i = i + 1;
            if (i >= teams)
                i = 0;

        }
        for(Map.Entry<UUID, Team> e : players.entrySet()){
            game.getPlayer(e.getKey()).setTeam(e.getValue());
        }
        return players;
    }

    public ChatColor chatColor() {
        return chatColor;
    }
}
