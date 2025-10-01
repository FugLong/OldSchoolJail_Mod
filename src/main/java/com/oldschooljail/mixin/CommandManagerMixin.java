package com.oldschooljail.mixin;

import com.mojang.brigadier.ParseResults;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.data.JailedPlayersData;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandManager.class)
public class CommandManagerMixin {
	
	@Inject(method = "execute", at = @At("HEAD"), cancellable = true)
	private void onExecuteCommand(ParseResults<ServerCommandSource> parseResults, String command, CallbackInfo ci) {
		if (!OldSchoolJailMod.getConfig().blockCommands) {
			return;
		}
		
		ServerCommandSource source = parseResults.getContext().getSource();
		ServerPlayerEntity player = source.getPlayer();
		
		if (player == null) {
			return; // Not a player
		}
		
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData == null || !jailedData.isJailed(player.getUuid())) {
			return; // Not jailed
		}
		
		// Get the command name
		String cmdName = command.trim().split(" ")[0].toLowerCase();
		if (cmdName.startsWith("/")) {
			cmdName = cmdName.substring(1);
		}
		
		// Allow /jail time command
		if (cmdName.equals("jail") && command.contains("time")) {
			return;
		}
		
		// Block all other commands (chat is not a command, so it's automatically allowed)
		player.sendMessage(Text.literal("§cYou cannot use commands while jailed! Use /jail time to check your sentence."), false);
		ci.cancel();
	}
}

