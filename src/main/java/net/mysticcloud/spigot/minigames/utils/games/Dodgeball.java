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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class Dodgeball extends Game {

    Map<Team, Item> flags = new HashMap<>();

    private long STARTED;


    public Dodgeball(Arena arena, int teams) {
        super("Dodgeball", arena);
        setGameState(new DodgeballGameState());
        setTeams(teams);
        setMinPlayers(teams);
        setMaxPlayers(teams * 4);
        setFriendlyFire(false);
        setController(new GameController() {

            Map<Team, ArrayList<UUID>> teamListMap = new HashMap<>();
            long LASTED = 0;

            @Override
            public void start() {
                STARTED = new Date().getTime();
                Map<UUID, Team> teamAssignments = Team.sort(getGameState().getPlayers().keySet(), getTeams(), Dodgeball.this);
                for (UUID uid : getGameState().getPlayers().keySet()) {
                    Objective ob = getScoreboards().get(uid).getScoreboard().registerNewObjective("score", Criteria.DUMMY, "score");
                    ob.setDisplaySlot(DisplaySlot.BELOW_NAME);
                    ob.setDisplayName(ChatColor.GREEN + Symbols.STAR_1.toString());
                    ob.getScore(Bukkit.getPlayer(uid).getName()).setScore(0);
                    getGameState().spawnPlayer(Bukkit.getPlayer(uid));
                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                LASTED = new Date().getTime() - STARTED;

                teamListMap.clear();
                for (GamePlayer player : getGameState().getPlayers().values()) {
                    if (player.getTeam().equals(Team.NONE) || player.getTeam().equals(Team.SPECTATOR)) continue;
                    if (!teamListMap.containsKey(player.getTeam()))
                        teamListMap.put(player.getTeam(), new ArrayList<>());
                    teamListMap.get(player.getTeam()).add(player.getUUID());
                }
                return teamListMap.size() == 1;

                //check scores and timer
            }


            @Override
            public JSONObject end() {
                int z = getGameState().getTeamScores().size();
                JSONObject extra = new JSONObject("{}");
                Map<Integer, Team> placements = new HashMap<>();

                for (Map.Entry<Team, Integer> entry : getGameState().sortTeamScores().entrySet()) {
                    placements.put(z, entry.getKey());
                    z = z - 1;
                }

                sendMessage(MessageUtils.colorize("&7--------------------------"));
                sendMessage("");
                sendMessage("");
                if (placements.isEmpty()) {
                    sendMessage(ChatColor.RED + "There was a draw.");
                } else {
                    if (placements.containsKey(1))
                        sendMessage("  " + placements.get(1).chatColor() + placements.get(1).name() + "&8 came in 1st place!");
                    if (placements.containsKey(2))
                        sendMessage("  " + placements.get(2).chatColor() + placements.get(2).name() + "&8 came in 2nd place!");
                    if (placements.containsKey(3))
                        sendMessage("  " + placements.get(3).chatColor() + placements.get(3).name() + "&8 came in 3rd place!");
                }
                sendMessage("");
                sendMessage("");
                sendMessage(MessageUtils.colorize("&7--------------------------"));

                return extra;
            }

            @Override
            public void generate() {
                JSONObject data = arena.getData();
                JSONArray spawns = arena.getData().getJSONArray("spawns");
                for (int i = 0; i < spawns.length(); i++) {
                    JSONObject spawnData = spawns.getJSONObject(i);
                    Location loc = Utils.decryptLocation(arena.getWorld(), spawnData.getJSONObject("location"));
                    addNoBuildZone(loc);
                }

            }


        });
    }


    public class DodgeballGameState extends GameState {
        @Override
        public void spawnPlayer(Player player) {
            super.spawnPlayer(player);
            Team team = getPlayer(player.getUniqueId()).getTeam();
            ItemStack hat = new ItemStack(Material.LEATHER_HELMET);
            ItemStack shirt = new ItemStack(Material.LEATHER_CHESTPLATE);
            ItemStack pants = new ItemStack(Material.LEATHER_LEGGINGS);
            ItemStack shoes = new ItemStack(Material.LEATHER_BOOTS);

            LeatherArmorMeta hatMeta = (LeatherArmorMeta) hat.getItemMeta();
            hatMeta.setColor(team.getDyeColor());
            hat.setItemMeta(hatMeta);

            LeatherArmorMeta shirtMeta = (LeatherArmorMeta) shirt.getItemMeta();
            shirtMeta.setColor(team.getDyeColor());
            shirt.setItemMeta(shirtMeta);

            LeatherArmorMeta pantsMeta = (LeatherArmorMeta) pants.getItemMeta();
            pantsMeta.setColor(team.getDyeColor());
            pants.setItemMeta(pantsMeta);

            LeatherArmorMeta shoesMeta = (LeatherArmorMeta) shoes.getItemMeta();
            shoesMeta.setColor(team.getDyeColor());
            shoes.setItemMeta(shoesMeta);

            player.getEquipment().setArmorContents(new ItemStack[]{shoes, pants, shirt, hat});

            player.getInventory().addItem(new ItemStack(Material.BOW));


        }

        @Override
        public void kill(Player player, EntityDamageEvent.DamageCause cause) {
            GamePlayer gamePlayer = getPlayer(player.getUniqueId());
            Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
            if (entity instanceof Player) {
                Player killer = Bukkit.getPlayer(entity.getUniqueId());
                score(killer);
                if (!killer.getInventory().contains(Material.ARROW))
                    killer.getInventory().addItem(new ItemStack(Material.ARROW));
            }
            defaultDeathMessages(player, cause);
            Firework rocket = spawnFirework(player.getLocation().clone().add(0, 1, 0), FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.BALL).withColor(gamePlayer.getTeam().getDyeColor()).build());
            rocket.detonate();
            super.kill(player, cause);
            getScoreboards().get(player.getUniqueId()).getScoreboard().getObjective("score").getScore(player.getName()).setScore(gamePlayer.getLives());
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
    }
}