package com.oldschooljail.mixin;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	
	@Shadow
	public ServerPlayerEntity player;
	
	// Monitor player movement and teleport back if they escape
	@Inject(method = "tick", at = @At("TAIL"))
	private void onTick(CallbackInfo ci) {
		if (!OldSchoolJailMod.getConfig().blockTeleportation) {
			return;
		}
		
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData == null || !jailedData.isJailed(player.getUuid())) {
			return;
		}
		
		JailedPlayer jailedPlayer = jailedData.getJailedPlayer(player.getUuid());
		JailData jailData = OldSchoolJailMod.getJailData();
		Jail jail = jailData.getJail(jailedPlayer.getJailName());
		
		if (jail == null) {
			// Jail doesn't exist anymore, release player
			jailedData.releasePlayer(player.getUuid());
			return;
		}
		
		// Check if player is too far from jail (more than 50 blocks)
		BlockPos jailPos = jail.getPosition();
		double distance = player.getPos().distanceTo(
			jailPos.getX() + 0.5,
			jailPos.getY(),
			jailPos.getZ() + 0.5
		);
		
		if (distance > 50) {
			// Teleport back to jail
			player.teleport(
				player.getServerWorld(),
				jailPos.getX() + 0.5,
				jailPos.getY(),
				jailPos.getZ() + 0.5,
				player.getYaw(),
				player.getPitch()
			);
			player.sendMessage(Text.literal("§cYou cannot escape from jail!"), true);
		}
	}
}

