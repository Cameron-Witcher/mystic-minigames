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
import org.bukkit.*;
import org.bukkit.block.Structure;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
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
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lives: " + player.getLives() + " | Score: " + getScore(Bukkit.getPlayer(player.getUUID())) + "/" + MAX_SCORE));
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
                JSONArray spawns = arena.getData().getJSONArray("spawns");
                for (int i = 0; i < spawns.length(); i++) {
                    JSONObject spawnData = spawns.getJSONObject(i);
                    Location loc = Utils.decryptLocation(arena.getWorld(), spawnData.getJSONObject("location"));
                    addNoBuildZone(loc);
                }
            }
        });
    }


    @Override
    protected void spawnPlayer(Player player) {
        super.spawnPlayer(player);
        player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.WOODEN_SWORD), new ItemStack(Material.ARROW));
    }

    @Override
    public void kill(Player player, EntityDamageEvent.DamageCause cause) {

        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
        Bukkit.broadcastMessage("1");
        if (entity instanceof Player) {
            Bukkit.broadcastMessage("2");
            Player killer = Bukkit.getPlayer(entity.getUniqueId());
            Bukkit.broadcastMessage("3");
            score(killer);
            Bukkit.broadcastMessage("4");
            killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.5f);
            killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 1.11f);
            killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1f, 0.95f);
            Bukkit.broadcastMessage("5");
            if (!killer.getInventory().contains(Material.ARROW))
                killer.getInventory().addItem(new ItemStack(Material.ARROW));
            Bukkit.broadcastMessage("6");
        }
        Bukkit.broadcastMessage("7");
        String victim = (gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName();
        Bukkit.broadcastMessage("8");
        String action = " was killed";
        Bukkit.broadcastMessage("9");
        String ending = "!";
        switch (cause) {
            case PROJECTILE:
                Bukkit.broadcastMessage("10");
                action = " was shot";
                Bukkit.broadcastMessage("11");
                ending = (entity == null ? " by a projectile!" : " by " + (entity instanceof Player ? (getPlayer(entity.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(entity.getUniqueId()).getTeam().chatColor()) : "&7") + entity.getName() + "&e!&7 (" + CoreUtils.distance(player.getLocation(), entity.getLocation()).intValue() + " blocks)");
                Bukkit.broadcastMessage("12");
                break;
            case VOID:
                Bukkit.broadcastMessage("13");
                action = " fell out of the world";
                Bukkit.broadcastMessage("14");
                ending = ".";
                Bukkit.broadcastMessage("15");
                if (entity != null) {
                    Bukkit.broadcastMessage("16");
                    Player killer = (Player) entity;
                    Bukkit.broadcastMessage("17");
                    action = " was pushed over the edge";
                    Bukkit.broadcastMessage("18");
                    ending = " by " + (getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                    Bukkit.broadcastMessage("19");
                }
                Bukkit.broadcastMessage("20");
                break;
            default:
                Bukkit.broadcastMessage("21");
                if (entity != null) {
                    Bukkit.broadcastMessage("22");
                    ending = " by &7" + entity.getName() + "&e.";
                    Bukkit.broadcastMessage("23");
                    if (entity instanceof Player) {
                        Bukkit.broadcastMessage("24");
                        Player killer = (Player) entity;
                        Bukkit.broadcastMessage("25");
                        if (killer.getEquipment() != null && killer.getEquipment().getItemInMainHand().getType().name().endsWith("_AXE")) {
                            Bukkit.broadcastMessage("26");
                            action = " was decapitated";
                            Bukkit.broadcastMessage("27");
                        }
                        Bukkit.broadcastMessage("28");
                        ending = " by " + (getPlayer(killer.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(killer.getUniqueId()).getTeam().chatColor()) + entity.getName() + "&e.";
                        Bukkit.broadcastMessage("29");
                    }
                    Bukkit.broadcastMessage("30");
                }
                Bukkit.broadcastMessage("31");
                break;
        }
        Bukkit.broadcastMessage("32");
        sendMessage("&3" + victim + "&e" + action + ending);
        Bukkit.broadcastMessage("33");
        Firework rocket = spawnFirework(player.getLocation(), FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.BALL).withColor(Color.RED).build());
        rocket.detonate();
        super.kill(player, cause);
    }
}