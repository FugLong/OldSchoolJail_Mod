package com.oldschooljail.mixin;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityAfkMixin {
	
	// Store original AFK positions for jailed players
	private static final Map<UUID, Vec3d> originalAfkPositions = new HashMap<>();
	// Track previous jail state to detect transitions
	private static final Map<UUID, Boolean> previousJailState = new HashMap<>();
	
	@Inject(method = "tick", at = @At("HEAD"))
	private void onTick(CallbackInfo ci) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		UUID playerUuid = player.getUuid();
		
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		boolean currentlyJailed = jailedData != null && jailedData.isJailed(playerUuid);
		boolean previouslyJailed = previousJailState.getOrDefault(playerUuid, false);
		
		// Update previous state
		previousJailState.put(playerUuid, currentlyJailed);
		
		// Only restore AFK position when transitioning from jailed to not-jailed
		if (!currentlyJailed && previouslyJailed && originalAfkPositions.containsKey(playerUuid)) {
			try {
				Object playerData = player.getClass().getMethod("ec$getPlayerData").invoke(player);
				if (playerData != null) {
					java.lang.reflect.Field lastTickPosField = playerData.getClass().getDeclaredField("lastTickPos");
					lastTickPosField.setAccessible(true);
					lastTickPosField.set(playerData, originalAfkPositions.get(playerUuid));
					
					// Remove from our storage since player is no longer jailed
					originalAfkPositions.remove(playerUuid);
					
					OldSchoolJailMod.LOGGER.debug("Restored original AFK position for released player {}", player.getName().getString());
				}
			} catch (Exception e) {
				// Essentials not present or different version, ignore
				originalAfkPositions.remove(playerUuid); // Clean up anyway
			}
			return;
		}
		
		// If not jailed, don't do anything else
		if (!currentlyJailed) {
			return;
		}
		
		JailedPlayer jailedPlayer = jailedData.getJailedPlayer(player.getUuid());
		JailData jailData = OldSchoolJailMod.getJailData();
		Jail jail = jailData.getJail(jailedPlayer.getJailName());
		
		if (jail == null) {
			return; // Jail doesn't exist
		}
		
		// Override Essentials Commands AFK position
		try {
			Object playerData = player.getClass().getMethod("ec$getPlayerData").invoke(player);
			if (playerData != null) {
				java.lang.reflect.Field lastTickPosField = playerData.getClass().getDeclaredField("lastTickPos");
				lastTickPosField.setAccessible(true);
				
				// Store original AFK position if we haven't already
				if (!originalAfkPositions.containsKey(playerUuid)) {
					Vec3d currentAfkPos = (Vec3d) lastTickPosField.get(playerData);
					originalAfkPositions.put(playerUuid, currentAfkPos);
					OldSchoolJailMod.LOGGER.debug("Stored original AFK position for jailed player {}: {}", player.getName().getString(), currentAfkPos);
				}
				
				// Override with jail position
				lastTickPosField.set(playerData, new Vec3d(jail.getX(), jail.getY(), jail.getZ()));
			}
		} catch (Exception e) {
			// Essentials not present or different version, ignore
			OldSchoolJailMod.LOGGER.debug("Could not override Essentials AFK position: {}", e.getMessage());
		}
	}
}
