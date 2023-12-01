package net.mysticcloud.spigot.minigames.commands;

import net.mysticcloud.spigot.core.commands.listeners.AdminCommandTabCompleter;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.minigames.MysticMinigames;
import net.mysticcloud.spigot.minigames.commands.listeners.GameCommandTabCompleter;
import net.mysticcloud.spigot.minigames.utils.Game;
import net.mysticcloud.spigot.minigames.utils.GameManager;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

public class GameCommands implements CommandExecutor {
    public GameCommands(MysticMinigames plugin, String... cmd) {
        for (String s : cmd) {
            PluginCommand com = plugin.getCommand(s);
            com.setExecutor(this);
            com.setTabCompleter(new GameCommandTabCompleter());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equals("game")) {
            if (args.length == 0) {
                //send help
                return true;
            }
            if (args[0].equalsIgnoreCase("create")) {
                //Syntax: /game create <game> <arena> [teams]
                if (args.length <= 2) {
                    sender.sendMessage(MessageUtils.prefixes("game") + "Usage: /game create <game> <arena> [teams]");
                    return true;
                }
                Game game = GameManager.createGame(args[1], new Arena(args[2]), args.length == 4 ? Integer.parseInt(args[3]) : -1);

            }
            if (args[0].equalsIgnoreCase("list")) {
                String s = "";
                for (Game game : GameManager.getGames().values())
                    s = s == "" ? game.getName() : s + ", " + game.getName();
                sender.sendMessage(MessageUtils.prefixes("game") + "Current games are: " + s);

            }
            if (args[0].equalsIgnoreCase("leave")) {
                if (sender instanceof Player)
                    for (MetadataValue metadataValue : ((Player) sender).getMetadata("game")) {
                        Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), () -> {
                            ((Game) metadataValue.value()).removePlayer(((Player) sender).getUniqueId());
                        }, 1);
                    }
                else sender.sendMessage(MessageUtils.prefixes("game") + "Sorry that is a player only command.");
            }
            if (args[0].equalsIgnoreCase("join")) {
                if (sender instanceof Player) {
                    if (args.length == 1) {
                        sender.sendMessage(MessageUtils.prefixes("game") + "Usage: /game join <game>");
                        return true;
                    }
                    Game game = GameManager.getGame(args[1]);
                    game.addPlayer(((Player) sender).getUniqueId());

                } else sender.sendMessage(MessageUtils.prefixes("game") + "Sorry that is a player only command.");
            }
        }
        return true;
    }
}
