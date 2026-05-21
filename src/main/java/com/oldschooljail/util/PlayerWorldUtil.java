package com.oldschooljail.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/** World/server access that works on Minecraft 1.21.2 through 1.21.11. */
public final class PlayerWorldUtil {

	private PlayerWorldUtil() {
	}

	public static ServerWorld getWorld(ServerPlayerEntity player) {
		return (ServerWorld) player.getEntityWorld();
	}

	public static MinecraftServer getServer(ServerPlayerEntity player) {
		return getWorld(player).getServer();
	}

	public static String getWorldId(ServerPlayerEntity player) {
		return getWorld(player).getRegistryKey().getValue().toString();
	}
}
