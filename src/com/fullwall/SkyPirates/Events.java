package com.fullwall.SkyPirates;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;

import com.fullwall.SkyPirates.BoatHandler.Mode;

public class Events implements Listener {
    private final SkyPirates plugin;

    public Events(final SkyPirates plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!(p.getVehicle() instanceof Boat) || !plugin.hasBoat(p.getVehicle()))
            return;
        BoatHandler boat = plugin.getBoatHandler(p.getVehicle());
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            boat.doRightClick();
        else if (boat.delay == 0)
            boat.doArmSwing();
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof Boat) || !(event.getVehicle().getPassenger() instanceof Player)) {
            return;
        }
        if (!plugin.hasBoat(event.getVehicle()))
            return;

        BoatHandler boat = plugin.getBoatHandler(event.getVehicle());
        boat.doYaw(event.getFrom(), event.getTo());
        // boat.doRealisticFriction();

        if (boat.isMoving()) {
            Vector vel = event.getVehicle().getVelocity();
            boat.movementHandler(vel);
            event.getVehicle().setVelocity(vel);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player))
            return;
        if (!(event.getVehicle() instanceof Boat))
            return;
        Player player = (Player) event.getEntered();
        if (!Permission.has(player, "skypirates.player.enable"))
            return;

        BoatHandler boat = plugin.getBoatHandler(player, (Boat) event.getVehicle());
        player.sendMessage(ChatColor.AQUA + "You feel a tingling sensation as you step into the boat.");
        boat.setMode(plugin.getMode(player));
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player) || !(event.getVehicle() instanceof Boat))
            return;
        if (!plugin.hasBoat(event.getVehicle()))
            return;
        Player player = (Player) event.getExited();
        if (player.getItemInHand() != null) {
            event.setCancelled(true);
            return;
        }
        BoatHandler boat = plugin.getBoatHandler(event.getVehicle());
        player.sendMessage(ChatColor.LIGHT_PURPLE + "The tingling disappears as you hop out.");

        boat.setMode(Mode.NORMAL);
    }

    @EventHandler
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (!(event.getVehicle() instanceof Boat) || !(event.getVehicle().getPassenger() instanceof Player)) {
            return;
        }

        if (!plugin.hasBoat(event.getVehicle()))
            return;

        Player p = (Player) event.getVehicle().getPassenger();

        if (!Permission.has(p, "skypirates.admin.invincible")
                && !(p.getItemInHand().getType() == Material.OBSIDIAN && Permission.has(p, "skypirates.items.obsidian")))
            return;
        event.setDamage(0);
        event.setCancelled(true);
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (plugin.hasBoat(event.getVehicle()) || !(event.getVehicle().getPassenger() instanceof Player)) {
            return;
        }
        Player p = (Player) event.getVehicle().getPassenger();

        if (!Permission.has(p, "skypirates.admin.invincible")
                && !(p.getItemInHand().getType() == Material.OBSIDIAN && Permission.has(p, "skypirates.items.obsidian")))
            return;
        event.setCancelled(true);
    }
}