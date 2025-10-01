package com.oldschooljail.util;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionUtil {
	public static final String JAIL_PLAYER = "oldschooljail.jail";
	public static final String RELEASE_PLAYER = "oldschooljail.release";
	public static final String SET_JAIL = "oldschooljail.set";
	public static final String DELETE_JAIL = "oldschooljail.delete";
	public static final String JAIL_IMMUNE = "oldschooljail.immune";
	public static final String JAIL_TIME = "oldschooljail.time";
	
	public static boolean hasPermission(ServerCommandSource source, String permission) {
		// Check if source is a player
		if (source.getPlayer() == null) {
			// Console/command blocks always have permission
			return true;
		}
		
		// Use Fabric Permissions API (works with LuckPerms, etc.)
		// Falls back to OP level 2 if no permission plugin is installed
		return Permissions.check(source, permission, 2);
	}
	
	public static boolean hasPermission(ServerPlayerEntity player, String permission) {
		return Permissions.check(player, permission, 2);
	}
	
	public static boolean isImmune(ServerPlayerEntity player) {
		return hasPermission(player, JAIL_IMMUNE);
	}
}

