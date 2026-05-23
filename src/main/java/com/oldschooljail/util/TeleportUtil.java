package com.oldschooljail.util;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.model.Jail;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Set;

public final class TeleportUtil {

	private TeleportUtil() {
	}

	public static void teleportToJail(ServerPlayer player, Jail jail, MinecraftServer server) {
		ServerLevel level = resolveLevel(server, jail.getWorldId());
		player.teleportTo(level, jail.getX(), jail.getY(), jail.getZ(), Set.of(), jail.getYaw(), jail.getPitch(), true);
	}

	public static void teleportToSavedLocation(
		ServerPlayer player,
		MinecraftServer server,
		double x,
		double y,
		double z,
		float yaw,
		float pitch,
		String worldId
	) {
		try {
			ServerLevel level = resolveLevel(server, worldId);
			player.teleportTo(level, x, y, z, Set.of(), yaw, pitch, true);
		} catch (Exception e) {
			OldSchoolJailMod.LOGGER.error("Failed to teleport player to saved location", e);
		}
	}

	private static ServerLevel resolveLevel(MinecraftServer server, String worldId) {
		ResourceKey<Level> levelKey = ResourceKey.create(
			Registries.DIMENSION,
			Identifier.parse(worldId)
		);
		ServerLevel level = server.getLevel(levelKey);
		return level != null ? level : server.overworld();
	}
}
