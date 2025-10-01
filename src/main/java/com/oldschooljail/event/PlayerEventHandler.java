package com.oldschooljail.event;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class PlayerEventHandler {
	
	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
			
			if (jailedData == null) return;
			
			JailedPlayer jailedPlayer = jailedData.getJailedPlayer(player.getUuid());
			
			if (jailedPlayer != null) {
				if (jailedPlayer.shouldBeReleased()) {
					// Sentence expired while offline - release and teleport back
					player.sendMessage(Text.literal(OldSchoolJailMod.getConfig().jailExpiredMessage));
					jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
					jailedData.releasePlayer(player.getUuid());
				} else {
					// Still jailed - teleport to jail
					JailData jailData = OldSchoolJailMod.getJailData();
					Jail jail = jailData.getJail(jailedPlayer.getJailName());
					
					if (jail != null) {
						teleportToJail(player, jail, server);
						long remaining = jailedPlayer.getRemainingTimeSeconds();
						player.sendMessage(Text.literal("§cYou are still jailed! Time remaining: " + formatTime(remaining)));
					} else {
						// Jail was deleted while offline - release them
						player.sendMessage(Text.literal("§aYour jail was removed. You are free!"));
						jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
						jailedData.releasePlayer(player.getUuid());
					}
				}
			}
		});
	}
	
	private static void teleportToJail(ServerPlayerEntity player, Jail jail, net.minecraft.server.MinecraftServer server) {
		try {
			net.minecraft.registry.RegistryKey<net.minecraft.world.World> worldKey = net.minecraft.registry.RegistryKey.of(
				net.minecraft.registry.RegistryKeys.WORLD,
				net.minecraft.util.Identifier.of(jail.getWorldId())
			);
			
			net.minecraft.server.world.ServerWorld world = server.getWorld(worldKey);
			if (world == null) {
				world = server.getOverworld();
			}
			
			// Use exact position and rotation from jail
			player.teleport(world, jail.getX(), jail.getY(), jail.getZ(), java.util.Set.of(), jail.getYaw(), jail.getPitch(), true);
		} catch (Exception e) {
			OldSchoolJailMod.LOGGER.error("Failed to teleport player to jail", e);
		}
	}
	
	private static String formatTime(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		long secs = seconds % 60;
		
		if (hours > 0) {
			return hours + "h " + minutes + "m " + secs + "s";
		} else if (minutes > 0) {
			return minutes + "m " + secs + "s";
		} else {
			return secs + "s";
		}
	}
}

