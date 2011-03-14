package com.fullwall.SkyPirates;

import com.nijikokun.bukkit.Permissions.Permissions;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Permission {
	@SuppressWarnings("unused")
	private static Permissions permissionsPlugin;
	private static boolean permissionsEnabled = false;

	public static void initialize(Server server) {
		Plugin test = server.getPluginManager().getPlugin("Permissions");
		if (test != null) {
			Logger log = Logger.getLogger("Minecraft");
			permissionsPlugin = (Permissions) test;
			permissionsEnabled = true;
			log.log(Level.INFO, "[SkyPirates]: Permissions enabled.");
		} else {
			permissionsEnabled = false;
			Logger log = Logger.getLogger("Minecraft");
			log.log(Level.SEVERE,
					"[SkyPirates]: Nijikokuns' Permissions plugin isn't loaded, everyone can use all features.");
		}
	}

	public static boolean isAdmin(Player player) {
		if (permissionsEnabled) {
			return permission(player, "skypirates.admin");
		}
		return player.isOp();
	}

	private static boolean permission(Player player, String string) {
		return Permissions.Security.permission(player, string);
	}

	public static boolean hasEnable(Player player) {
		if (permissionsEnabled) {
			return permission(player, "skypirates.enable");
		}
		return true;
	}

	public static boolean genericCheck(Player player, String str) {
		if (permissionsEnabled) {
			return permission(player, str);
		}
		return true;
	}
}