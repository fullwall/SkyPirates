package com.fullwall.skypirates;

import org.bukkit.command.CommandSender;

public class Permission {
    public static boolean has(CommandSender player, String str) {
        return player.hasPermission(str);
    }
}