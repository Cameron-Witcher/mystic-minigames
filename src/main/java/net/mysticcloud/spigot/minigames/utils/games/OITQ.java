package net.mysticcloud.spigot.minigames.utils.games;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.util.*;

public class OITQ extends Game {

    int MAX_SCORE = 50;
    int MAX_LIVES = 5;


    public OITQ(Arena arena) {
        super("OITQ", arena);
        setTeams(1);
        setMinPlayers(2);
        setMaxPlayers(20);
        setController(new GameController() {

            List<UUID> players = new ArrayList<>();

            @Override
            public void start() {
                for (UUID uid : getPlayers().keySet()) {
                    getPlayer(uid).setMaxLives(MAX_LIVES);
                    spawnPlayer(Bukkit.getPlayer(uid));
                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                players.clear();
                for (GamePlayer player : getPlayers().values()) {
                    if (player.getTeam().equals(Team.SPECTATOR)) continue;
                    if (getScore(player.getTeam()) >= MAX_SCORE) return true;
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lives: " + player.getLives() + " | Team Score: " + getScore(player.getTeam()) + "/" + MAX_SCORE));
                    players.add(player.getUUID());
                }
                return players.size() <= 1;

                //check scores and timer
            }


            @Override
            public void end() {
                int z = getPlayerScores().size();
                for (Map.Entry<UUID, Integer> entry : sortPlayerScores().entrySet()) {
                    if (z == 1) {
                        sendMessage("&3" + Bukkit.getPlayer(entry.getKey()).getName() + "&7 came in 1st place!");
                    }
                    z = z - 1;
                    //Divvy rewards and send messages
                }
            }

            @Override
            public void generate() {
                JSONArray save = RegionUtils.getSave(arena.getName());
                Location loc = new Location(arena.getWorld(), 0, 0, 0);
                int length = 0, width = 0, height = 0;
                for (int i = 0; i < save.length(); i++) {
                    JSONObject data = save.getJSONObject(i);
                    if (data.getInt("x") > length) length = data.getInt("x");
                    if (data.getInt("y") > height) height = data.getInt("y");
                    if (data.getInt("z") > width) width = data.getInt("z");

                    if (!Bukkit.createBlockData(data.getString("data")).getMaterial().equals(Material.STRUCTURE_BLOCK)) {
                        loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z")).getBlock().setBlockData(Bukkit.createBlockData(data.getString("data")));
                    } else {
                        Location bloc = loc.clone().add(data.getInt("x"), data.getInt("y"), data.getInt("z"));
                        JSONObject sdata = data.getJSONObject("structure_data");
                        switch (sdata.getString("structure")) {
                            case "location:spawn":
                                addNoBuildZone(bloc);
                                addSpawn(new Spawn(bloc, Team.NONE));
                                break;
                        }
                    }
                }
                arena.setDimentions(length, width, height);
                //Do shit
            }
        });
    }


    @Override
    protected void spawnPlayer(Player player) {
        super.spawnPlayer(player);
        player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.ARROW));
    }

    @Override
    public void kill(Player player, EntityDamageEvent.DamageCause cause) {
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
        if (!(entity == null) && entity instanceof Player) {
            score(Bukkit.getPlayer(entity.getUniqueId()));
        }
        String victim = (gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName();
        String action = " was killed";
        String ending = "!";
        switch (cause) {
            case PROJECTILE:
                action = " was shot";
                ending = (entity == null ? " by a projectile!" : " by " + (Bukkit.getEntity(entity.getUniqueId()) instanceof Player ? (getPlayer(entity.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(entity.getUniqueId()).getTeam().chatColor()) : "&7") + entity.getName() + "&e!&7 (" + CoreUtils.distance(player.getLocation(), entity.getLocation()).intValue() + " blocks)");
                break;
            case VOID:
                action = " fell out of the world";
                ending = ".";
                if (entity != null) {
                    Player killer = (Player) entity;
                    action = " was pushed over the edge";
                    ending = " by " + (getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                }
                break;
            default:
                if (entity != null) {
                    ending = " by &7" + entity.getName() + "&e.";
                    if (entity instanceof Player) {
                        Player killer = (Player) entity;
                        if (killer.getEquipment() != null && killer.getEquipment().getItemInMainHand().getType().name().endsWith("_AXE")) {
                            action = " was decapitated";
                        }
                        ending = " by " + (getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                    }
                }
                break;
        }
        sendMessage("&3" + victim + "&e" + action + ending);
        super.kill(player, cause);
    }
}