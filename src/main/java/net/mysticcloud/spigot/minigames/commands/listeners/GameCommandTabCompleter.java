package net.mysticcloud.spigot.minigames.commands.listeners;

import net.mysticcloud.spigot.minigames.utils.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameCommandTabCompleter implements TabCompleter {

    Map<String, List<String>> cmds = new HashMap<>();

    public GameCommandTabCompleter() {
        cmds.put("create", new ArrayList<>());
        cmds.put("edit", new ArrayList<>());
        cmds.put("list", new ArrayList<>());
        cmds.put("join", new ArrayList<>());
        cmds.put("help", new ArrayList<>(cmds.keySet()));


    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (cmd.getName().equalsIgnoreCase("game")) {
            if (args.length == 1)
                StringUtil.copyPartialMatches(args[0], new ArrayList<>(cmds.keySet()), completions);

            if (args.length >= 2) {
                if (args[0].equalsIgnoreCase("edit"))
                    cmds.put("edit", new ArrayList<>(GameManager.getGames().keySet()));

                if (args[0].equalsIgnoreCase("join"))
                    cmds.put("join", new ArrayList<>(GameManager.getGames().keySet()));

                for (String s : cmds.keySet()) {
                    if (args[0].equalsIgnoreCase(s)) {
                        StringUtil.copyPartialMatches(args[1], cmds.get(s), completions);
                    }
                }
            }
        }


        return completions;

    }

    public List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            players.add(player.getName());
        }
        return players;
    }

}
