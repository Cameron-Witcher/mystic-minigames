package net.mysticcloud.spigot.minigames.utils.games;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mysticcloud.spigot.core.utils.CoreUtils;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.util.*;

public class OITQ extends Game {

    int MAX_SCORE = 50;
    int MAX_LIVES = 5;

    private Objective livesObjective = getScoreboardManager().getScoreboard().registerNewObjective("lives", "dummy", "Lives");


    public OITQ(Arena arena) {
        super("OITQ", arena);
        setGameState(new OITQGameState());
        setTeams(1);
        setMinPlayers(2);
        setMaxPlayers(20);

        setFriendlyFire(true);

        livesObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        livesObjective.setDisplayName(ChatColor.RED + Symbols.HEART_1.toString());


        setController(new GameController() {

            List<UUID> players = new ArrayList<>();

            @Override
            public void start() {
                for (UUID uid : getGameState().getPlayers().keySet()) {
                    livesObjective.getScore(Bukkit.getPlayer(uid).getName()).setScore(MAX_LIVES);
                    getGameState().getPlayer(uid).setMaxLives(MAX_LIVES);
                    getGameState().spawnPlayer(Bukkit.getPlayer(uid));
                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                players.clear();
                for (GamePlayer player : getGameState().getPlayers().values()) {
                    if (player.getTeam().equals(Team.SPECTATOR)) continue;
                    if (getGameState().getScore(player.getTeam()) >= MAX_SCORE) return true;
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.DARK_AQUA + "Lives: " + player.getLives() + " | Score: " + getGameState().getScore(Bukkit.getPlayer(player.getUUID())) + "/" + MAX_SCORE));
                    players.add(player.getUUID());
                }
                return players.size() <= 1;

                //check scores and timer
            }


            @Override
            public JSONObject end() {
                JSONObject extra = new JSONObject("{}");

                int z = getGameState().getPlayerScores().size();
                Map<Integer, UUID> placements = new HashMap<>();



                for (Map.Entry<UUID, Integer> entry : getGameState().sortPlayerScores().entrySet()) {
                    placements.put(z, entry.getKey());
                    extra.put(entry.getKey().toString(), new JSONObject("{}"));
                    extra.getJSONObject(entry.getKey().toString()).put("deaths", MAX_LIVES - getGameState().getPlayer(entry.getKey()).getLives());
                    z = z - 1;
                }

                sendMessage(MessageUtils.colorize("&7--------------------------"));
                sendMessage("");
                sendMessage("");
                if (placements.isEmpty()) {
                    sendMessage(ChatColor.RED + "There was a draw.");
                } else {
                    if (placements.containsKey(1))
                        sendMessage("  " + "&b" + Bukkit.getPlayer(placements.get(1)).getName() + "&8 came in 1st place!");
                    if (placements.containsKey(2))
                        sendMessage("  " + "&6" + Bukkit.getPlayer(placements.get(2)).getName() + "&8 came in 2nd place!");
                    if (placements.containsKey(3))
                        sendMessage("  " + "&7" + Bukkit.getPlayer(placements.get(3)).getName() + "&8 came in 3rd place!");
                }
                sendMessage("");
                sendMessage("");
                sendMessage(MessageUtils.colorize("&7--------------------------"));
                return extra;
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

    public class OITQGameState extends GameState {
        @Override
        public int score(Player player, int amount) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.11f);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.95f);
            return super.score(player, amount);
        }

        @Override
        public int score(Team team, int amount) {
            for (UUID uid : getPlayers(team)) {
                Player player = Bukkit.getPlayer(uid);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.11f);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.95f);
            }
            return super.score(team, amount);
        }

        @Override
        public void processDamage(Player victim, double damage, EntityDamageEvent.DamageCause cause) {
            if (cause.equals(EntityDamageEvent.DamageCause.PROJECTILE)) damage = 100;
            super.processDamage(victim, damage, cause);
        }

        @Override
        public void spawnPlayer(Player player) {
            super.spawnPlayer(player);
            player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.WOODEN_SWORD), new ItemStack(Material.ARROW));
        }

        @Override
        public void kill(Player player, EntityDamageEvent.DamageCause cause) {

            GamePlayer gamePlayer = getGameState().getPlayer(player.getUniqueId());
            Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
            if (entity instanceof Player) {
                Player killer = Bukkit.getPlayer(entity.getUniqueId());
                getGameState().score(killer);
                killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.11f);
                killer.playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.95f);
                if (!killer.getInventory().contains(Material.ARROW))
                    killer.getInventory().addItem(new ItemStack(Material.ARROW));
            }
            defaultDeathMessages(player, cause);
            Firework rocket = spawnFirework(player.getLocation().clone().add(0, 1, 0), FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.BALL).withColor(Color.RED).build());
            rocket.detonate();
            super.kill(player, cause);
            livesObjective.getScore(player.getName()).setScore(gamePlayer.getLives());
        }
    }


}