package com.oldschooljail.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class PermissionUtil {
	public static final String JAIL_PLAYER = "oldschooljail.jail";
	public static final String RELEASE_PLAYER = "oldschooljail.release";
	public static final String SET_JAIL = "oldschooljail.set";
	public static final String DELETE_JAIL = "oldschooljail.delete";
	public static final String JAIL_IMMUNE = "oldschooljail.immune";
	public static final String JAIL_TIME = "oldschooljail.time";

	private static final String PERMISSIONS_CLASS = "me.lucko.fabric.api.permissions.v0.Permissions";
	private static final String TRISTATE_CLASS = "net.fabricmc.fabric.api.util.TriState";

	private static Boolean permissionsApiAvailable;

	public static boolean hasPermission(CommandSourceStack source, String permission) {
		if (source.getPlayer() == null) {
			return true;
		}

		Boolean fabricPerms = getFabricPermission(source, permission);
		if (fabricPerms != null) {
			return fabricPerms;
		}

		return isOperator(source);
	}

	/** Permission for jailed players using {@code /jail time} on themselves. Defaults to allowed when no permissions plugin is present. */
	public static boolean hasJailTimePermission(CommandSourceStack source) {
		Boolean fabricPerms = getFabricPermission(source, JAIL_TIME);
		if (fabricPerms != null) {
			return fabricPerms;
		}
		return true;
	}

	public static boolean isImmune(ServerPlayer player) {
		Boolean fabricPerms = getFabricPermission(player, JAIL_IMMUNE);
		if (fabricPerms != null) {
			return fabricPerms;
		}

		// Without a permissions plugin, operators cannot be jailed (prevents locking out admins).
		return isOperatorByOpList(player);
	}

	private static boolean isPermissionsApiOnClasspath() {
		if (permissionsApiAvailable != null) {
			return permissionsApiAvailable;
		}
		try {
			Class.forName(PERMISSIONS_CLASS);
			permissionsApiAvailable = true;
		} catch (ClassNotFoundException e) {
			permissionsApiAvailable = false;
		}
		return permissionsApiAvailable;
	}

	private static Boolean getFabricPermission(Object target, String permission) {
		if (!isPermissionsApiOnClasspath()) {
			return null;
		}
		try {
			Class<?> permissionsClass = Class.forName(PERMISSIONS_CLASS);
			Class<?> triStateClass = Class.forName(TRISTATE_CLASS);
			Object state = permissionsClass
				.getMethod("getPermissionValue", Object.class, String.class)
				.invoke(null, target, permission);
			Object defaultState = Enum.valueOf((Class<Enum>) triStateClass, "DEFAULT");
			if (!state.equals(defaultState)) {
				Object trueState = Enum.valueOf((Class<Enum>) triStateClass, "TRUE");
				return state.equals(trueState);
			}
		} catch (Throwable ignored) {
		}
		return null;
	}

	private static boolean isOperator(CommandSourceStack source) {
		ServerPlayer player = source.getPlayer();
		if (player == null) {
			return true;
		}

		if (hasPermissionLevel(source, 1)) {
			return true;
		}

		if (hasModeratorPermissions(source)) {
			return true;
		}

		return isOperatorByOpList(player);
	}

	private static boolean hasPermissionLevel(CommandSourceStack source, int level) {
		try {
			return (boolean) source.getClass().getMethod("hasPermission", int.class).invoke(source, level);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}

	private static boolean isOperatorByOpList(ServerPlayer player) {
		MinecraftServer server = PlayerWorldUtil.getServer(player);
		if (server == null) {
			return false;
		}

		String playerName = player.getGameProfile().name();
		for (String opName : server.getPlayerList().getOpNames()) {
			if (playerName.equalsIgnoreCase(opName)) {
				return true;
			}
		}
		return false;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static boolean hasModeratorPermissions(CommandSourceStack source) {
		try {
			Class<?> permissionLevelClass = Class.forName("net.minecraft.commands.permission.PermissionLevel");
			Class<?> levelPermissionClass = Class.forName("net.minecraft.commands.permission.Permission$Level");
			Class<?> permissionClass = Class.forName("net.minecraft.commands.permission.Permission");

			Object moderatorLevel = Enum.valueOf((Class) permissionLevelClass, "MODERATORS");
			Object levelPermission = levelPermissionClass.getConstructor(permissionLevelClass).newInstance(moderatorLevel);
			Object permissions = source.getClass().getMethod("getPermissions").invoke(source);
			return (boolean) permissions.getClass().getMethod("hasPermission", permissionClass).invoke(permissions, levelPermission);
		} catch (ReflectiveOperationException ignored) {
			return false;
		}
	}
}
