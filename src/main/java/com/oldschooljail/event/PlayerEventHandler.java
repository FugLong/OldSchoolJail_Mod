package com.oldschooljail.event;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import com.oldschooljail.util.TeleportUtil;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class PlayerEventHandler {

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();

			if (jailedData == null) return;

			JailedPlayer jailedPlayer = jailedData.getJailedPlayer(player.getUUID());

			if (jailedPlayer != null) {
				if (jailedPlayer.shouldBeReleased()) {
					player.sendSystemMessage(Component.literal(OldSchoolJailMod.getConfig().jailExpiredMessage));
					if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
						jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
					}
					jailedData.releasePlayer(player.getUUID());
				} else {
					JailData jailData = OldSchoolJailMod.getJailData();
					if (jailData == null) {
						return;
					}
					Jail jail = jailData.getJail(jailedPlayer.getJailName());

					if (jail != null) {
						TeleportUtil.teleportToJail(player, jail, server);
						long remaining = jailedPlayer.getRemainingTimeSeconds();
						player.sendSystemMessage(Component.literal("§cYou are still jailed! Time remaining: " + formatTime(remaining)));
					} else {
						player.sendSystemMessage(Component.literal("§aYour jail was removed. You are free!"));
						if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
							jailedData.teleportToOriginalLocation(player, jailedPlayer, server);
						}
						jailedData.releasePlayer(player.getUUID());
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
