package com.oldschooljail.mixin;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailedPlayersData;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public abstract class ServerPlayerInteractionManagerMixin {
	
	@Shadow
	public ServerPlayerEntity player;
	
	// Block breaking
	@Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
	private void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (OldSchoolJailMod.getConfig().blockBlockBreaking) {
			JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
			if (jailedData != null && jailedData.isJailed(player.getUuid())) {
				player.sendMessage(Text.literal("§cYou cannot break blocks while jailed!"), true);
				cir.setReturnValue(false);
			}
		}
	}
	
	// Block interaction (buttons, levers, etc.)
	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, 
								BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		if (OldSchoolJailMod.getConfig().blockInteraction) {
			JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
			if (jailedData != null && jailedData.isJailed(player.getUuid())) {
				player.sendMessage(Text.literal("§cYou cannot interact with blocks while jailed!"), true);
				cir.setReturnValue(ActionResult.FAIL);
			}
		}
	}
	
	// Block placing
	@Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
	private void onInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, 
								CallbackInfoReturnable<ActionResult> cir) {
		if (OldSchoolJailMod.getConfig().blockBlockPlacing) {
			JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
			if (jailedData != null && jailedData.isJailed(player.getUuid())) {
				// Check if player is trying to place a block
				if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
					player.sendMessage(Text.literal("§cYou cannot place blocks while jailed!"), true);
					cir.setReturnValue(ActionResult.FAIL);
				}
			}
		}
	}
}

