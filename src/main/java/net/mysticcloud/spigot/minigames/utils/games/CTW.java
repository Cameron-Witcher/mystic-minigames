package net.mysticcloud.spigot.minigames.utils.games;

import net.mysticcloud.spigot.core.utils.regions.RegionUtils;
import net.mysticcloud.spigot.minigames.utils.Team;
import net.mysticcloud.spigot.minigames.utils.games.arenas.Arena;
import net.mysticcloud.spigot.minigames.utils.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Structure;
import org.bukkit.entity.Player;
import org.json2.JSONArray;
import org.json2.JSONObject;

import java.util.Map;
import java.util.UUID;

public class CTW extends Game {
    public CTW(Arena arena, int teams) {
        super("CTW", arena);
        setTeams(teams);
        setMinPlayers(teams);
        setMaxPlayers(teams * 4);
        setController(new GameController() {
            @Override
            public void start() {

                Team.sort(getPlayers(), getTeams());
                for (Map.Entry<UUID, Team> e : getPlayers().entrySet()) {
                    Player player = Bukkit.getPlayer(e.getKey());
                    switch (e.getValue()) {
                        case RED:
                            player.teleport((Location) getData().get("red_spawn"));
                            break;
                        case BLUE:
                            player.teleport((Location) getData().get("blue_spawn"));
                            break;
                    }
                }
                //Teleport players to team spawns
            }

            @Override
            public boolean check() {
                //check scores and timer
                return false;
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
                            case "ctw:red_spawn":
                                data.put("red_spawn", bloc);
                                break;
                            case "ctw:blue_spawn":
                                data.put("blue_spawn", bloc);
                                break;
                        }
                    }
                }
                arena.setDimentions(length, width, height);
                //Do shit
            }
        });
    }
}