package com.fullwall.SkyPirates;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.fullwall.SkyPirates.BoatHandler.Mode;

/**
 * SkyPirates for Bukkit
 * 
 * @author fullwall
 */
public class SkyPirates extends JavaPlugin {
    private final Map<Player, Mode> playerModes = new HashMap<Player, Mode>();
    private final Map<Integer, BoatHandler> boats = new HashMap<Integer, BoatHandler>();

    public static SkyPirates plugin;

    private static final String codename = "Caribbean";
    public static final Logger log = Logger.getLogger("Minecraft");

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new Events(this), this);

        PluginDescriptionFile pdfFile = this.getDescription();

        getLogger().info("version [" + pdfFile.getVersion() + "] (" + codename + ") loaded");

    }

    @Override
    public void onDisable() {
        PluginDescriptionFile pdfFile = this.getDescription();
        getLogger().info("version [" + pdfFile.getVersion() + "] (" + codename + ") disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("[Skypirates]: Must be ingame to use this command.");
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0)
            return false;
        if (args[0].equals("clear") || args[0].equals("c")) {
            if (Permission.has(player, "skypirates.admin.clear")) {
                BoatHandler b;
                if (boats.isEmpty()) {
                    player.sendMessage(ChatColor.GRAY + "There are no SkyPirates boats to remove.");
                    return true;
                }
                for (Entry<Integer, BoatHandler> entry : boats.entrySet()) {
                    b = entry.getValue();
                    if (b.boat.isEmpty()) {
                        b.boat.remove();
                        boats.remove(entry.getKey());
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "You don't have permission to use that command.");
                return true;
            }
            return true;
        } else if (args[0].equals("help")) {
            if (!(Permission.has(player, "skypirates.player.help"))) {
                player.sendMessage(ChatColor.DARK_RED + "You don't have permission to use that command.");
                return true;
            }
            player.sendMessage(ChatColor.AQUA + "SkyPirates Modes List");
            player.sendMessage(ChatColor.YELLOW + "---------------------");
            player.sendMessage(ChatColor.GREEN + "plane|p - " + ChatColor.AQUA + "turns your boat into a plane.");
            player.sendMessage(ChatColor.GREEN + "submarine|sub|s - " + ChatColor.AQUA
                    + "turns your boat into a submersible.");
            player.sendMessage(ChatColor.GREEN + "hoverboat|h - " + ChatColor.AQUA
                    + "turns your boat into a hoverboat.");
            player.sendMessage(ChatColor.GREEN + "glider|g - " + ChatColor.AQUA + "turns your boat into a glider.");
            player.sendMessage(ChatColor.GREEN + "drill|d - " + ChatColor.AQUA + "turns your boat into a drill.");
            player.sendMessage(ChatColor.GREEN + "anything else - " + ChatColor.AQUA
                    + "turns your boat back into the regular old jumping variety.");
            player.sendMessage(ChatColor.YELLOW + "---------------------");
            return true;
        }
        if (!args[0].equalsIgnoreCase("mode"))
            return false;
        if (!player.isInsideVehicle()) {
            player.sendMessage(ChatColor.RED + "Modes must be changed within a boat.");
            return true;
        }
        if (!(player.getVehicle() instanceof Boat) && !hasBoat(player.getVehicle())) {
            player.sendMessage(ChatColor.RED + "Modes must be changed within a boat.");
            return true;
        }
        BoatHandler boat = getBoatHandler(player.getVehicle());
        if (!Permission.has(player, "skypirates.player.changemode")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to do that.");
            return true;
        }
        if (args[1].equals("p") || args[1].equals("plane")) {
            if (Permission.has(player, "skypirates.modes.plane")) {
                player.sendMessage(ChatColor.GREEN
                        + "The boat feels suddenly weightless, like a breath of wind would carry you away!");
                playerModes.put(player, Mode.PLANE);
                boat.setMode(Mode.PLANE);
                return true;
            } else {
                player.sendMessage(ChatColor.RED
                        + "As much as you will it to float, the boat remains stubbornly on the ground.");
                return true;
            }
        } else if (args[1].equals("s") || args[1].contains("sub")) {
            if (Permission.has(player, "skypirates.modes.submarine")) {
                playerModes.put(player, Mode.SUBMARINE);
                player.sendMessage(ChatColor.BLUE
                        + "You feel the boat getting heavier and heavier as you sink beneath the waves.");
                boat.setMode(Mode.SUBMARINE);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "As hard as you try, the boat refuses to sink below the water.");
                return true;
            }
        } else if (args[1].contains("hover") || args[1].equals("h")) {
            if (Permission.has(player, "skypirates.modes.hoverboat")) {
                player.sendMessage(ChatColor.GOLD + "The boat lifts into the air, hovering over the world below.");
                playerModes.put(player, Mode.HOVERBOAT);
                boat.setMode(Mode.HOVERBOAT);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "The boat retains its usual weight.");
                return true;
            }
        } else if (args[1].contains("glider") || args[1].equals("g")) {
            if (Permission.has(player, "skypirates.modes.glider")) {
                player.sendMessage(ChatColor.WHITE + "The boat prepares to float gently downwards.");
                playerModes.put(player, Mode.GLIDER);
                boat.setMode(Mode.GLIDER);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "The boat retains its usual weight.");
                return true;
            }
        } else if (args[1].contains("drill") || args[1].equals("d")) {
            if (Permission.has(player, "skypirates.modes.drill")) {
                player.sendMessage(ChatColor.DARK_GRAY
                        + "The boat feels like it has immense force behind it, enough to drill through solid earth.");
                playerModes.put(player, Mode.DRILL);
                boat.setMode(Mode.DRILL);
                return true;
            } else {
                player.sendMessage(ChatColor.RED + "The boat retains its usual strength.");
                return true;
            }
        } else {
            player.sendMessage(ChatColor.GRAY + "The boat is just that, an ordinary vehicle.");
            playerModes.put(player, Mode.NORMAL);
            boat.setMode(Mode.NORMAL);
            return true;
        }
    }

    public BoatHandler getBoatHandler(Entity boat) {
        if (boat == null)
            return null;
        return boats.get(boat.getEntityId());
    }

    public boolean hasBoat(Entity boat) {
        if (boat == null)
            return false;
        return boats.containsKey(boat.getEntityId());
    }

    public Mode getMode(Player player) {
        if (!playerModes.containsKey(player))
            playerModes.put(player, Mode.NORMAL);
        return playerModes.get(player);
    }

    public BoatHandler getBoatHandler(Player player, Boat vehicle) {
        if (!hasBoat(vehicle)) {
            boats.put(vehicle.getEntityId(), new BoatHandler(vehicle, getMode(player)));
        }
        return boats.get(vehicle.getEntityId());
    }
}