package com.oldschooljail.util;

import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
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
		if (source.getPlayer() == null) {
			return true;
		}

		if (FabricLoader.getInstance().isModLoaded("luckperms")) {
			try {
				TriState state = me.lucko.fabric.api.permissions.v0.Permissions.getPermissionValue(source, permission);
				if (state != TriState.DEFAULT) {
					return state == TriState.TRUE;
				}
			} catch (Throwable ignored) {
			}
		}

		return isOperator(source);
	}

	public static boolean isImmune(ServerPlayerEntity player) {
		if (FabricLoader.getInstance().isModLoaded("luckperms")) {
			try {
				TriState state = me.lucko.fabric.api.permissions.v0.Permissions.getPermissionValue(
					(Entity) player, JAIL_IMMUNE);
				if (state != TriState.DEFAULT) {
					return state == TriState.TRUE;
				}
			} catch (Throwable ignored) {
			}
		}

		// Without a permissions plugin, operators are treated as immune (same as before).
		return isOperatorByOpList(player);
	}

	private static boolean isOperator(ServerCommandSource source) {
		ServerPlayerEntity player = source.getPlayer();
		if (player == null) {
			return true;
		}

		try {
			var method = source.getClass().getMethod("hasPermissionLevel", int.class);
			if ((boolean) method.invoke(source, 1)) {
				return true;
			}
		} catch (ReflectiveOperationException ignored) {
		}

		if (hasModeratorPermissions(source)) {
			return true;
		}

		return isOperatorByOpList(player);
	}

	private static boolean isOperatorByOpList(ServerPlayerEntity player) {
		MinecraftServer server = PlayerWorldUtil.getServer(player);
		if (server == null) {
			return false;
		}

		String playerName = player.getName().getString();
		for (String opName : server.getPlayerManager().getOpList().getNames()) {
			if (playerName.equalsIgnoreCase(opName)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static boolean hasModeratorPermissions(ServerCommandSource source) {
		try {
			Class<?> permissionLevelClass = Class.forName("net.minecraft.command.permission.PermissionLevel");
			Class<?> levelPermissionClass = Class.forName("net.minecraft.command.permission.Permission$Level");
			Class<?> permissionClass = Class.forName("net.minecraft.command.permission.Permission");

			Object moderatorLevel = Enum.valueOf((Class) permissionLevelClass, "MODERATORS");
			Object levelPermission = levelPermissionClass.getConstructor(permissionLevelClass).newInstance(moderatorLevel);
			Object permissions = source.getClass().getMethod("getPermissions").invoke(source);
			return (boolean) permissions.getClass().getMethod("hasPermission", permissionClass).invoke(permissions, levelPermission);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}
}
