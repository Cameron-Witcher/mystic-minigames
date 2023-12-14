package net.mysticcloud.spigot.minigames.utils.games;

import dev.sergiferry.playernpc.api.NPC;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.placeholder.Symbols;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.Utils;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.util.Vector;
import org.json2.JSONArray;
import org.json2.JSONObject;
import org.w3c.dom.Text;

import java.util.*;
import java.util.concurrent.TimeUnit;

import net.mysticcloud.spigot.core.utils.holograms.*;

public class CTW extends Game {


    int MAX_SCORE = 5;
    int MAX_LIVES = 10;

    List<ItemGenerator> generators = new ArrayList<ItemGenerator>();

    private final long MAX_DURATION = TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);


    public CTW(Arena arena, int teams) {
        super("CTW", arena);
        setGameState(new CTWGameState());
        setTEAMS(teams);
        setMIN_PLAYERS(teams);
        setMAX_PLAYERS(teams * 4);
        setFriendlyFire(false);


        setController(new GameController() {

            Map<Team, ArrayList<UUID>> teamListMap = new HashMap<>();


            @Override
            public void start() {
                LinkedList<String> sidebarList = new LinkedList<>();
                sidebarList.add("&1");
                sidebarList.add("&7&lSCORES&8:");
                for (int i = 0; i != teams; i++) {
                    Team team = Team.values()[i];
                    sidebarList.add(" " + team.chatColor() + "&l" + team.name() + "&8: " + team.chatColor() + "%team_" + team.name() + "_score%");
                }
                sidebarList.add("&2");
                Map<UUID, Team> teamAssignments = Team.sort(getGameState().getPlayers().keySet(), getTEAMS(), CTW.this);
                for (UUID uid : getGameState().getPlayers().keySet()) {
                    Objective ob1 = getScoreboards().get(uid).getScoreboard().registerNewObjective("lives", Criteria.DUMMY, "lives");
                    ob1.setDisplaySlot(DisplaySlot.BELOW_NAME);
                    ob1.setDisplayName(ChatColor.RED + Symbols.HEART_1.toString());
                    ob1.getScore(Bukkit.getPlayer(uid).getName()).setScore(MAX_LIVES);
                    getScoreboards().get(uid).sidebar(MessageUtils.colorize("&6" + getName() + " - " + arena.getName()), sidebarList);
                    getGameState().getPlayer(uid).setMaxLives(MAX_LIVES);
                    getGameState().spawnPlayer(Bukkit.getPlayer(uid));
                }
                for (Team team : teamAssignments.values()) {
                    ((CTWGameState) getGameState()).returnFlag(team, false);

                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;

                for (ItemGenerator gen : getGenerators()) {
                    gen.check();
                }

                teamListMap.clear();
                for (GamePlayer player : getGameState().getPlayers().values()) {
                    getScoreboards().get(player.getUUID()).update();
                    if (player.getTeam().equals(Team.NONE) || player.getTeam().equals(Team.SPECTATOR)) continue;
                    if (getGameState().getScore(player.getTeam()) >= MAX_SCORE) return true;
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(player.getTeam().chatColor() + "Lives: " + player.getLives() + " | " + MessageUtils.formatTimeRaw(MAX_DURATION - getGameState().getCurrentDuration()) + " | Team Score: " + getGameState().getScore(player.getTeam()) + "/" + MAX_SCORE));
                    if (!teamListMap.containsKey(player.getTeam()))
                        teamListMap.put(player.getTeam(), new ArrayList<>());
                    teamListMap.get(player.getTeam()).add(player.getUUID());
                }
                return teamListMap.size() == 1 || getGameState().getCurrentDuration() >= MAX_DURATION;

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

                JSONArray flags = arena.getData().getJSONArray("flags");
                for (int i = 0; i < flags.length(); i++) {
                    JSONObject flagData = flags.getJSONObject(i);
                    Location loc = Utils.decryptLocation(arena.getWorld(), flagData.getJSONObject("location"));
                    loc.getBlock().setType(Material.OAK_FENCE);
                    addNoBuildZone(loc);
                    Team team = Team.valueOf(flagData.getString("team").toUpperCase());
                    getData().put(team.name().toLowerCase() + "_flag", loc);
                }

                if (arena.getData().has("generators")) {
                    JSONArray generators = arena.getData().getJSONArray("generators");
                    for (int i = 0; i < generators.length(); i++) {
                        JSONObject generatorData = generators.getJSONObject(i);
                        Location loc = Utils.decryptLocation(arena.getWorld(), generatorData.getJSONObject("location"));
                        addNoBuildZone(loc);
                        getGenerators().add(new ItemGenerator(loc));

                    }
                }
            }


        });
    }

    public List<ItemGenerator> getGenerators() {
        return generators;
    }


    public class CTWGameState extends GameState {

        Map<Team, Item> flags = new HashMap<>();

        public void returnFlag(Team team, boolean message) {
            if (flags.containsKey(team)) flags.get(team).remove();
            Location loc = ((Location) getData().get(team.name().toLowerCase() + "_flag"));
            World world = loc.getWorld();
            assert world != null;
            Item item = world.dropItem(loc.getBlock().getLocation().clone().add(0.5, 1.51, 0.5), new ItemStack(Material.valueOf(team.name() + "_WOOL")));
            item.setVelocity(new Vector(0, 0, 0));
            item.setUnlimitedLifetime(true);
            item.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
            item.setInvulnerable(true);
            flags.put(team, item);
            if (message)
                sendMessage(MessageUtils.colorize("&eThe " + team.chatColor() + "&l" + team.name() + "&r&e flag has been returned!"));
        }

        private void dropFlag(Team team, Player player) {
            if (flags.containsKey(team)) flags.get(team).remove();
            GamePlayer gamePlayer = getPlayer(player.getUniqueId());
            World world = player.getWorld();
            assert world != null;
            Item item = world.dropItem(player.getLocation(), new ItemStack(Material.valueOf(team.name() + "_WOOL")));
            item.setVelocity(new Vector(0, 0, 0));
            item.setUnlimitedLifetime(true);
            item.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
            item.setInvulnerable(true);
            flags.put(team, item);
            sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas dropped the " + team.chatColor() + "&l" + team.name() + "&r&e flag!"));
            item.setMetadata("rogue_flag", new FixedMetadataValue(Utils.getPlugin(), Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), new RogueFlagTracker(item), 0)));


        }


        public void pickupFlag(Player player, Item item) {
            Team team = (Team) item.getMetadata("flag").get(0).value();
            if (flags.containsKey(team)) flags.get(team).remove();
            GamePlayer gamePlayer = getGameState().getPlayer(player.getUniqueId());

            sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas stolen the " + team.chatColor() + "&l" + team.name() + "&r&e flag!"));
            player.getEquipment().setHelmet(item.getItemStack());
            player.playSound(player, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
            item.remove();
            player.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
        }

        public void captureFlag(Player player, Team flag) {
            getGameState().score(getGameState().getPlayer(player.getUniqueId()).getTeam());
            GamePlayer gamePlayer = getGameState().getPlayer(player.getUniqueId());

            sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas captured the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag! (&7" + getGameState().getScore(gamePlayer.getTeam()) + "/" + MAX_SCORE + "&e)"));
            ItemStack hat = new ItemStack(Material.LEATHER_HELMET);

            LeatherArmorMeta hatMeta = (LeatherArmorMeta) hat.getItemMeta();
            hatMeta.setColor(gamePlayer.getTeam().getDyeColor());
            hat.setItemMeta(hatMeta);

            player.getEquipment().setHelmet(hat);

            player.removeMetadata("flag", Utils.getPlugin());

            returnFlag(flag, false);


        }

        @Override
        public void removePlayer(UUID uid, boolean list) {
            Player player = Bukkit.getPlayer(uid);
            if (player.hasMetadata("flag")) {

                Team flag = (Team) player.getMetadata("flag").get(0).value();

                returnFlag(flag, true);
                sendMessage(MessageUtils.colorize(getPlayer(uid).getTeam().chatColor() + "&l" + player.getName() + "&r &ehas dropped the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag!"));
                player.removeMetadata("flag", Utils.getPlugin());
            }
            super.removePlayer(uid, list);

        }

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

            player.getInventory().addItem(new ItemStack(Material.BOW), new ItemStack(Material.IRON_SWORD), new ItemStack(Material.IRON_AXE), new ItemStack(Material.IRON_SHOVEL), new ItemStack(Material.IRON_PICKAXE), new ItemStack(Material.OAK_PLANKS, 64), new ItemStack(Material.OAK_PLANKS, 64), new ItemStack(Material.ARROW, 64), new ItemStack(Material.BREAD, 64));


        }

        @Override
        public void kill(Player player, EntityDamageEvent.DamageCause cause) {
            GamePlayer gamePlayer = getPlayer(player.getUniqueId());
            Entity entity = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
            if (entity instanceof Player) score(Bukkit.getPlayer(entity.getUniqueId()));

            defaultDeathMessages(player, cause);
            if (player.hasMetadata("flag")) {

                Team flag = (Team) player.getMetadata("flag").get(0).value();
                if (player.getLocation().getY() < 0) returnFlag(flag, true);
                else dropFlag(flag, player);

                player.removeMetadata("flag", Utils.getPlugin());
            }
            Firework rocket = spawnFirework(player.getLocation().clone().add(0, 1, 0), FireworkEffect.builder().flicker(true).with(FireworkEffect.Type.BALL).withColor(gamePlayer.getTeam().getDyeColor()).build());
            rocket.detonate();
            super.kill(player, cause);
            getScoreboards().get(player.getUniqueId()).getScoreboard().getObjective("lives").getScore(player.getName()).setScore(gamePlayer.getLives());
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

        private class RogueFlagTracker implements Runnable {
            Item item;
            Team team;
            long DROPPED;

            public RogueFlagTracker(Item item) {
                this.item = item;
                this.team = (Team) item.getMetadata("flag").get(0).value();
                this.DROPPED = new Date().getTime();
            }

            @Override
            public void run() {

                if (new Date().getTime() - DROPPED >= TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS)) {
                    if (!item.isDead() || !item.isEmpty()) {
                        ((CTWGameState) getGameState()).returnFlag(team, true);
                    } else {
                        returnFlag(team, false);
                    }
                } else Bukkit.getScheduler().runTaskLater(Utils.getPlugin(), this, 1);
            }
        }
    }


    private class ItemGenerator {
        final Location loc;
        long DELAY;
        long LAST_DROP = 0;
        final ClassicHologram holo;

        public ItemGenerator(Location loc) {
            this(loc, TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES));
        }

        public ItemGenerator(Location loc, long delay) {
            loc.setPitch(0);
            loc.setYaw(0);
            this.loc = loc.getBlock().getLocation().add(0.5, 0.5, 0.5);
            this.DELAY = delay;
            this.holo = HologramManager.createClassicHologram(loc.clone().add(0, 1.55, 0));
            holo.setLine(0, "&a&lEmerald Generator");
            holo.setLine(1, "&7-----------------");
            holo.setLine(2, "&a00&7:&a00");
            holo.setLine(3, "&7-----------------");
        }

        public void changeDelay(long delay) {
            this.DELAY = delay;
        }

        public boolean check() {
            long NOW = new Date().getTime();
            holo.setLine(2, MessageUtils.colorize(MessageUtils.formatTime(DELAY - (NOW - LAST_DROP), "&a", "&7")));
            if (NOW - LAST_DROP >= DELAY) {
                LAST_DROP = NOW;
                Item item = loc.getWorld().dropItem(loc, new ItemStack(Material.EMERALD));
                item.setVelocity(new Vector(0, 0, 0));

                return true;
            }
            return false;
        }

    }
}