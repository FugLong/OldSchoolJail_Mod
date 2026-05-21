package com.oldschooljail.util;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.model.Jail;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.Set;

public final class TeleportUtil {

	private TeleportUtil() {
	}

	public static void teleportToJail(ServerPlayerEntity player, Jail jail, MinecraftServer server) {
		ServerWorld world = resolveWorld(server, jail.getWorldId());
		player.teleport(world, jail.getX(), jail.getY(), jail.getZ(), Set.of(), jail.getYaw(), jail.getPitch(), true);
	}

	public static void teleportToSavedLocation(
		ServerPlayerEntity player,
		MinecraftServer server,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		String worldId
	) {
		try {
			ServerWorld world = resolveWorld(server, worldId);
			player.teleport(world, x, y, z, Set.of(), yaw, pitch, true);
		} catch (Exception e) {
			OldSchoolJailMod.LOGGER.error("Failed to teleport player to saved location", e);
		}
	}

	private static ServerWorld resolveWorld(MinecraftServer server, String worldId) {
		RegistryKey<World> worldKey = RegistryKey.of(
			RegistryKeys.WORLD,
			Identifier.of(worldId)
		);
		ServerWorld world = server.getWorld(worldKey);
		return world != null ? world : server.getOverworld();
	}
}
