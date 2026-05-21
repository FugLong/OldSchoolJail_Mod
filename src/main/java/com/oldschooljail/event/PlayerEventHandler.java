package com.oldschooljail.event;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import com.oldschooljail.util.TeleportUtil;
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
					// Sentence expired while offline - release and teleport back (if enabled)
					player.sendMessage(Text.literal(OldSchoolJailMod.getConfig().jailExpiredMessage));
					if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
						jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
					}
					jailedData.releasePlayer(player.getUuid());
				} else {
					// Still jailed - teleport to jail
					JailData jailData = OldSchoolJailMod.getJailData();
					Jail jail = jailData.getJail(jailedPlayer.getJailName());
					
					if (jail != null) {
						TeleportUtil.teleportToJail(player, jail, server);
						long remaining = jailedPlayer.getRemainingTimeSeconds();
						player.sendMessage(Text.literal("§cYou are still jailed! Time remaining: " + formatTime(remaining)));
					} else {
						// Jail was deleted while offline - release them
						player.sendMessage(Text.literal("§aYour jail was removed. You are free!"));
						if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
							jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
						}
						jailedData.releasePlayer(player.getUuid());
					}
				}
			}
		});
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

