package net.mysticcloud.spigot.minigames.utils.games;

import net.mysticcloud.spigot.core.utils.MessageUtils;
import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Structure;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.util.*;

public class CTW extends Game {
    public CTW(Arena arena, int teams) {
        super("CTW", arena);
        setTeams(teams);
        setMinPlayers(teams);
        setMaxPlayers(teams * 4);
        setController(new GameController() {

            Map<Team, ArrayList<UUID>> teamListMap = new HashMap<>();

            @Override
            public void start() {
                Team.sort(getPlayers().keySet(), getTeams(), CTW.this);
                for (UUID uid : getPlayers().keySet()) {
                    spawnPlayer(Bukkit.getPlayer(uid));
                }
            }

            @Override
            public boolean check() {
                if (!getGameState().hasStarted()) return false;
                teamListMap.clear();
                for (GamePlayer player : getPlayers().values()) {
                    if (player.getTeam().equals(Team.NONE) || player.getTeam().equals(Team.SPECTATOR)) continue;
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
                        }
                    }
                }
                arena.setDimentions(length, width, height);
                //Do shit
            }
        });
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
}