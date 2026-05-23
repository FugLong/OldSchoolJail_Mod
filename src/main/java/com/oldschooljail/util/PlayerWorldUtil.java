package com.oldschooljail.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** World/server access for Minecraft 26.1.x. */
public final class PlayerWorldUtil {

	private PlayerWorldUtil() {
	}

	public static ServerLevel getLevel(ServerPlayer player) {
		return (ServerLevel) player.level();
	}

	public static MinecraftServer getServer(ServerPlayer player) {
		return getLevel(player).getServer();
	}

	public static String getWorldId(ServerPlayer player) {
		return getLevel(player).dimension().identifier().toString();
	}
}
