package com.oldschooljail.util;

import net.fabricmc.fabric.api.util.TriState;
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
		
		// Use Fabric Permissions API
		TriState state = net.fabricmc.fabric.api.util.TriState.DEFAULT;
		try {
			state = (TriState) source.getClass()
				.getMethod("hasPermission", String.class, int.class)
				.invoke(source, permission, 2);
		} catch (Exception e) {
			// Fallback to op level if permissions API is not available
			return source.hasPermissionLevel(2);
		}
		
		// If state is DEFAULT, fall back to op level
		if (state == TriState.DEFAULT) {
			return source.hasPermissionLevel(2);
		}
		
		return state == TriState.TRUE;
	}
	
	public static boolean hasPermission(ServerPlayerEntity player, String permission) {
		return hasPermission(player.getCommandSource(), permission);
	}
	
	public static boolean isImmune(ServerPlayerEntity player) {
		return hasPermission(player, JAIL_IMMUNE);
	}
}

