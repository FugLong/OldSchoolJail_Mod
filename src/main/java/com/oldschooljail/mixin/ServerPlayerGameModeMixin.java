package com.oldschooljail.mixin;

import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailedPlayersData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {

	@Shadow
	public ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!OldSchoolJailMod.getConfig().blockBlockBreaking) {
			return;
		}
		if (isJailed(player)) {
			denyBreak(cir, "§cYou cannot break blocks while jailed!");
		}
	}

	@Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
	private void onUseItemOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
							 BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
		if (!OldSchoolJailMod.getConfig().blockInteraction) {
			return;
		}
		if (isJailed(player)) {
			denyInteraction(player, cir, "§cYou cannot interact with blocks while jailed!");
		}
	}

	@Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
	private void onUseItem(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand,
						   CallbackInfoReturnable<InteractionResult> cir) {
		if (!isJailed(player)) {
			return;
		}

		if (OldSchoolJailMod.getConfig().blockBlockPlacing && stack.getItem() instanceof BlockItem) {
			denyInteraction(player, cir, "§cYou cannot place blocks while jailed!");
			return;
		}

		if (OldSchoolJailMod.getConfig().blockTeleportation && isTeleportItem(stack)) {
			denyInteraction(player, cir, "§cYou cannot use teleport items while jailed!");
		}
	}

	private static boolean isJailed(ServerPlayer player) {
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		return jailedData != null && jailedData.isJailed(player.getUUID());
	}

	private static boolean isTeleportItem(ItemStack stack) {
		return stack.is(Items.ENDER_PEARL)
			|| stack.is(Items.CHORUS_FRUIT)
			|| stack.is(Items.WIND_CHARGE);
	}

	private void denyBreak(CallbackInfoReturnable<Boolean> cir, String message) {
		player.sendSystemMessage(Component.literal(message), true);
		cir.setReturnValue(false);
	}

	private static void denyInteraction(ServerPlayer player, CallbackInfoReturnable<InteractionResult> cir, String message) {
		player.sendSystemMessage(Component.literal(message), true);
		cir.setReturnValue(InteractionResult.FAIL);
	}
}
