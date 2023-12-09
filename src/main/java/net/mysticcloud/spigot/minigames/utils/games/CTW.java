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
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CTW extends Game {

    Map<Team, Item> flags = new HashMap<>();

    int MAX_SCORE = 5;
    int MAX_LIVES = 10;
    private long STARTED;
    private final long DURATION = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);

    private Objective livesObjective = getScoreboardManager().getScoreboard().registerNewObjective("lives", "dummy", "Lives");


    public CTW(Arena arena, int teams) {
        super("CTW", arena);
        setTeams(teams);
        setMinPlayers(teams);
        setMaxPlayers(teams * 4);
        setFriendlyFire(false);
        livesObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
        livesObjective.setDisplayName(ChatColor.RED + Symbols.HEART_1.toString());
        setController(new GameController() {

            Map<Team, ArrayList<UUID>> teamListMap = new HashMap<>();
            long LASTED = 0;

            @Override
            public void start() {
                STARTED = new Date().getTime();
                Map<UUID, Team> teamAssignments = Team.sort(getPlayers().keySet(), getTeams(), CTW.this);
                for (UUID uid : getPlayers().keySet()) {
                    livesObjective.getScore(Bukkit.getPlayer(uid).getName()).setScore(MAX_LIVES);
                    getPlayer(uid).setMaxLives(MAX_LIVES);
                    spawnPlayer(Bukkit.getPlayer(uid));
                }
                for (Team team : teamAssignments.values()) {
                    dropFlag(team, ((Location) getData().get(team.name().toLowerCase() + "_flag")));

                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                LASTED = new Date().getTime() - STARTED;

                teamListMap.clear();
                for (GamePlayer player : getPlayers().values()) {
                    if (player.getTeam().equals(Team.NONE) || player.getTeam().equals(Team.SPECTATOR)) continue;
                    if (getScore(player.getTeam()) >= MAX_SCORE) return true;
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(player.getTeam().chatColor() + "Lives: " + player.getLives() + " | " + MessageUtils.formatTimeRaw(DURATION - LASTED) + " | Team Score: " + getScore(player.getTeam()) + "/" + MAX_SCORE));
                    if (!teamListMap.containsKey(player.getTeam()))
                        teamListMap.put(player.getTeam(), new ArrayList<>());
                    teamListMap.get(player.getTeam()).add(player.getUUID());
                }
                return teamListMap.size() == 1 || LASTED >= DURATION;

                //check scores and timer
            }


            @Override
            public void end() {
                int z = getTeamScores().size();
                Map<Integer, Team> placements = new HashMap<>();

                for (Map.Entry<Team, Integer> entry : sortTeamScores().entrySet()) {
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

                JSONArray flags = arena.getData().getJSONArray("flags");
                for (int i = 0; i < flags.length(); i++) {
                    JSONObject flagData = flags.getJSONObject(i);
                    Location loc = Utils.decryptLocation(arena.getWorld(), flagData.getJSONObject("location"));
                    loc.getBlock().setType(Material.OAK_FENCE);
                    addNoBuildZone(loc);
                    Team team = Team.valueOf(flagData.getString("team").toUpperCase());
                    getData().put(team.name().toLowerCase() + "_flag", loc);
                }
            }


        });
    }

    @Override
    public void removePlayer(UUID uid, boolean list) {
        Player player = Bukkit.getPlayer(uid);
        if (player.hasMetadata("flag")) {

            Team flag = (Team) player.getMetadata("flag").get(0).value();

            dropFlag(flag, ((Location) getData().get(flag.name().toLowerCase() + "_flag")));
            sendMessage(MessageUtils.colorize(getPlayer(uid).getTeam().chatColor() + "&l" + player.getName() + "&r &ehas dropped the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag!"));
            player.removeMetadata("flag", Utils.getPlugin());
        }
        super.removePlayer(uid, list);

    }

    private void dropFlag(Team team, Location loc) {
        World world = loc.getWorld();
        assert world != null;
        Item item = world.dropItem(loc.getBlock().getLocation().clone().add(0.5, 1.51, 0.5), new ItemStack(Material.valueOf(team.name() + "_WOOL")));
        item.setVelocity(new Vector(0, 0, 0));
        item.setUnlimitedLifetime(true);
        item.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
        item.setInvulnerable(true);
        flags.put(team, item);
    }

    @Override
    protected void spawnPlayer(Player player) {
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

        player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_AXE), new ItemStack(Material.IRON_SHOVEL), new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.OAK_PLANKS, 64), new ItemStack(Material.OAK_PLANKS, 64), new ItemStack(Material.ARROW, 64), new ItemStack(Material.BREAD, 64));


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
        String victim = (gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName();
        String action = " was killed";
        String ending = "!";
        switch (cause) {
            case PROJECTILE:
                action = " was shot";
                ending = (entity == null ? " by a projectile!" : " by " + (entity instanceof Player ? (getPlayer(entity.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(entity.getUniqueId()).getTeam().chatColor()) : "&7") + entity.getName() + "&e!&7 (" + CoreUtils.distance(player.getLocation(), entity.getLocation()).intValue() + " blocks)");
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
        if (player.hasMetadata("flag")) {

            Team flag = (Team) player.getMetadata("flag").get(0).value();

            dropFlag(flag, ((Location) getData().get(flag.name().toLowerCase() + "_flag")));
            sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas dropped the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag!"));
            player.removeMetadata("flag", Utils.getPlugin());
        }
        Firework rocket = spawnFirework(player.getLocation().clone().add(0, 1, 0), FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.BALL).withColor(gamePlayer.getTeam().getDyeColor()).build());
        rocket.detonate();
        super.kill(player, cause);
        livesObjective.getScore(player.getName()).setScore(gamePlayer.getLives());
    }

    public void pickupFlag(Player player, Item item) {
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        Team team = (Team) item.getMetadata("flag").get(0).value();
        sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas stolen the " + team.chatColor() + "&l" + team.name() + "&r&e flag!"));
        player.getEquipment().setHelmet(item.getItemStack());
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
        item.remove();
        player.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
    }

    public void captureFlag(Player player, Team flag) {
        score(getPlayer(player.getUniqueId()).getTeam());
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());

        sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas captured the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag! (&7" + getScore(gamePlayer.getTeam()) + "/" + MAX_SCORE + "&e)"));
        ItemStack hat = new ItemStack(Material.LEATHER_HELMET);

        LeatherArmorMeta hatMeta = (LeatherArmorMeta) hat.getItemMeta();
        hatMeta.setColor(gamePlayer.getTeam().getDyeColor());
        hat.setItemMeta(hatMeta);

        player.getEquipment().setHelmet(hat);

        player.removeMetadata("flag", Utils.getPlugin());

        dropFlag(flag, ((Location) getData().get(flag.name().toLowerCase() + "_flag")));


    }

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
}