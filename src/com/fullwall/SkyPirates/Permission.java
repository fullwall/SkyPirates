package com.fullwall.SkyPirates;

import org.bukkit.entity.Player;

public class Permission {
    public static boolean has(Player player, String str) {
        return player.hasPermission(str);
    }
}