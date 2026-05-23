package com.oldschooljail.mixin;

import com.mojang.brigadier.ParseResults;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.util.JailedCommandUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {

	@Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
	private void onExecuteCommand(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
		if (!OldSchoolJailMod.getConfig().blockCommands) {
			return;
		}

		CommandSourceStack source = parseResults.getContext().getSource();
		ServerPlayer player = source.getPlayer();

		if (player == null) {
			return;
		}

		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData == null || !jailedData.isJailed(player.getUUID())) {
			return;
		}

		if (JailedCommandUtil.isSelfJailTimeCommand(parseResults)
			&& JailedCommandUtil.canJailedPlayerUseJailTime(source)) {
			return;
		}

		player.sendSystemMessage(Component.literal(
			"§cYou cannot use commands while jailed! Use /jail time to check your sentence."), false);
		ci.cancel();
	}
}
