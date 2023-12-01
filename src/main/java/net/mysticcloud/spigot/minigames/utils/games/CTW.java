package net.mysticcloud.spigot.minigames.utils.games;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

public class CTW extends Game {

    Map<Team, Item> flags = new HashMap<>();


    public CTW(Arena arena, int teams) {
        super("CTW", arena);
        setTeams(teams);
        setMinPlayers(teams);
        setMaxPlayers(teams * 4);
        setController(new GameController() {

            Map<Team, ArrayList<UUID>> teamListMap = new HashMap<>();

            @Override
            public void start() {
                Map<UUID, Team> teamAssignments = Team.sort(getPlayers().keySet(), getTeams(), CTW.this);
                for (UUID uid : getPlayers().keySet()) {
                    spawnPlayer(Bukkit.getPlayer(uid));
                }
                for (Team team : teamAssignments.values()) {
                    dropFlag(team, ((Location) getData().get(team.name().toLowerCase() + "_flag")));

                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                teamListMap.clear();
                for (GamePlayer player : getPlayers().values()) {
                    if (player.getTeam().equals(Team.NONE) || player.getTeam().equals(Team.SPECTATOR)) continue;
                    Bukkit.getPlayer(player.getUUID()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(ChatColor.RED + "Lives: " + player.getLives()));
                    if (!teamListMap.containsKey(player.getTeam()))
                        teamListMap.put(player.getTeam(), new ArrayList<>());
                    teamListMap.get(player.getTeam()).add(player.getUUID());
                }
                return teamListMap.size() == 1;

                //check scores and timer
            }


            @Override
            public void end() {
                //Divvy rewards and send messages
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
                            case "spawn:red":
                            case "ctw:red_spawn":
                                addSpawn(new Spawn(bloc, Team.RED));
                                break;
                            case "spawn:blue":
                            case "ctw:blue_spawn":
                                addSpawn(new Spawn(bloc, Team.BLUE));
                                break;
                            case "spawn:green":
                                addSpawn(new Spawn(bloc, Team.GREEN));
                                break;
                            case "spawn:yellow":
                                addSpawn(new Spawn(bloc, Team.YELLOW));
                                break;
                            case "spawn:none":
                            case "location:spawn":
                                addSpawn(new Spawn(bloc, Team.NONE));
                                break;
                            case "ctw:red_flag":
                                getData().put("red_flag", bloc);
                                bloc.getBlock().setType(Material.OAK_FENCE);
                                break;
                            case "ctw:blue_flag":
                                getData().put("blue_flag", bloc);
                                bloc.getBlock().setType(Material.OAK_FENCE);
                                break;
                        }
                    }
                }
                arena.setDimentions(length, width, height);
                //Do shit
            }
        });
    }

    private void dropFlag(Team team, Location loc) {
        World world = loc.getWorld();
        assert world != null;
        Item item = world.dropItem(loc.clone().add(0, 1.51, 0), new ItemStack(Material.valueOf(team.name() + "_WOOL")));
        item.setVelocity(new Vector(0, 0, 0));
        item.setUnlimitedLifetime(true);
        item.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
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
        Entity damager = player.hasMetadata("last_damager") ? Bukkit.getEntity((UUID) player.getMetadata("last_damager").get(0).value()) : null;
        switch (cause) {
            default:
                sendMessage((gamePlayer.getTeam().equals(Team.NONE) ? "&3" : gamePlayer.getTeam().chatColor()) + player.getName() + "&e was killed" + (damager == null ? "!" : " by " + (getPlayer(damager.getUniqueId()).getTeam().equals(Team.NONE) ? "&3" : getPlayer(damager.getUniqueId()).getTeam().chatColor()) + damager.getName() + "&e!"));
                break;
        }
        super.kill(player, cause);
    }

    public void pickupFlag(Player player, Item item) {
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        Team team = (Team) item.getMetadata("flag").get(0).value();
        sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas stolen the " + team.chatColor() + "&l" + team.name() + "&r&e flag!"));
        player.getEquipment().setHelmet(item.getItemStack());
        item.remove();
        player.setMetadata("flag", new FixedMetadataValue(Utils.getPlugin(), team));
    }

    public void captureFlag(Player player, Team flag) {
        GamePlayer gamePlayer = getPlayer(player.getUniqueId());
        sendMessage(MessageUtils.colorize(gamePlayer.getTeam().chatColor() + "&l" + player.getName() + "&r &ehas captured the " + flag.chatColor() + "&l" + flag.name() + "&r&e flag!"));
        ItemStack hat = new ItemStack(Material.LEATHER_HELMET);

        LeatherArmorMeta hatMeta = (LeatherArmorMeta) hat.getItemMeta();
        hatMeta.setColor(gamePlayer.getTeam().getDyeColor());
        hat.setItemMeta(hatMeta);

        player.getEquipment().setHelmet(hat);

        player.removeMetadata("flag", Utils.getPlugin());

        dropFlag(flag, ((Location) getData().get(flag.name().toLowerCase() + "_flag")));

    }
}