package com.fullwall.SkyPirates;

import org.bukkit.entity.Player;

public class Permission {

	public static boolean isAdmin(Player player) {
		return permission(player, "skypirates.admin");
	}

	public static boolean hasEnable(Player player) {
		return permission(player, "skypirates.enable");
	}

	public static boolean permission(Player player, String str) {
		return true;// player.hasPermission(str);
	}
}