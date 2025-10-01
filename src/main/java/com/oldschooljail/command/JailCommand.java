package com.oldschooljail.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.config.JailConfig;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import com.oldschooljail.util.PermissionUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class JailCommand {
	
	private static final SuggestionProvider<ServerCommandSource> JAIL_NAME_SUGGESTIONS = (context, builder) -> {
		JailData jailData = OldSchoolJailMod.getJailData();
		if (jailData != null) {
			return CommandSource.suggestMatching(
				jailData.getAllJails().keySet(),
				builder
			);
		}
		return builder.buildFuture();
	};
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("jail")
			// /jail <player> <time> <reason>
			.then(CommandManager.argument("player", EntityArgumentType.player())
				.then(CommandManager.argument("time", IntegerArgumentType.integer(1))
					.then(CommandManager.argument("reason", StringArgumentType.greedyString())
						.executes(JailCommand::jailPlayer))))
			
			// /jail set <name>
			.then(CommandManager.literal("set")
				.then(CommandManager.argument("name", StringArgumentType.word())
					.executes(JailCommand::setJailAtCurrentPos)
					// /jail set <name> <x> <y> <z>
					.then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
						.executes(JailCommand::setJailAtPos))))
			
			// /jail delete <name>
			.then(CommandManager.literal("delete")
				.then(CommandManager.argument("name", StringArgumentType.word())
					.suggests(JAIL_NAME_SUGGESTIONS)
					.executes(JailCommand::deleteJail)))
			
			// /jail release <player>
			.then(CommandManager.literal("release")
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(JailCommand::releasePlayer)))
			
			// /jail time
			.then(CommandManager.literal("time")
				.executes(JailCommand::checkJailTime))
		);
	}
	
	private static int jailPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		// Check permission
		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendError(Text.literal("§cYou don't have permission to jail players!"));
			return 0;
		}
		
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		int time = IntegerArgumentType.getInteger(context, "time");
		String reason = StringArgumentType.getString(context, "reason");
		
		// Check if target is immune
		if (PermissionUtil.isImmune(target)) {
			source.sendError(Text.literal("§cThat player is immune to jailing!"));
			return 0;
		}
		
		JailConfig config = OldSchoolJailMod.getConfig();
		long timeInSeconds = config.convertToSeconds(time);
		
		// Check max sentence
		if (timeInSeconds > config.maxSentenceSeconds) {
			source.sendError(Text.literal("§cJail time exceeds maximum allowed sentence of " + 
				config.maxSentenceSeconds + " seconds!"));
			return 0;
		}
		
		JailData jailData = OldSchoolJailMod.getJailData();
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		
		// Get a random jail or first available jail
		Jail jail = jailData.getAllJails().entrySet().stream()
			.findFirst()
			.map(e -> jailData.getJail(e.getKey()))
			.orElse(null);
		
		if (jail == null) {
			source.sendError(Text.literal("§cNo jails have been set! Use /jail set <name> first."));
			return 0;
		}
		
		// Jail the player
		long releaseTime = System.currentTimeMillis() + (timeInSeconds * 1000);
		String jailerName = source.getName();
		
		JailedPlayer jailedPlayer = new JailedPlayer(
			target.getUuid(),
			jail.getName(),
			releaseTime,
			reason,
			jailerName
		);
		
		jailedData.jailPlayer(jailedPlayer);
		
		// Teleport player to jail
		teleportToJail(target, jail);
		
		// Send messages
		String jailMsg = config.jailMessage
			.replace("%time%", formatTime(timeInSeconds))
			.replace("%jailer%", jailerName)
			.replace("%reason%", reason);
		
		target.sendMessage(Text.literal(jailMsg));
		source.sendFeedback(() -> Text.literal("§aJailed " + target.getName().getString() + 
			" for " + formatTime(timeInSeconds) + "!"), true);
		
		return 1;
	}
	
	private static int setJailAtCurrentPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		if (!PermissionUtil.hasPermission(source, PermissionUtil.SET_JAIL)) {
			source.sendError(Text.literal("§cYou don't have permission to set jails!"));
			return 0;
		}
		
		ServerPlayerEntity player = source.getPlayerOrThrow();
		String name = StringArgumentType.getString(context, "name");
		
		BlockPos pos = player.getBlockPos();
		RegistryKey<World> worldKey = player.getWorld().getRegistryKey();
		String worldId = worldKey.getValue().toString();
		
		Jail jail = new Jail(name, pos, worldId);
		OldSchoolJailMod.getJailData().addJail(jail);
		
		source.sendFeedback(() -> Text.literal("§aJail '" + name + "' set at " + 
			pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), true);
		
		return 1;
	}
	
	private static int setJailAtPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		if (!PermissionUtil.hasPermission(source, PermissionUtil.SET_JAIL)) {
			source.sendError(Text.literal("§cYou don't have permission to set jails!"));
			return 0;
		}
		
		String name = StringArgumentType.getString(context, "name");
		BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
		
		RegistryKey<World> worldKey = source.getWorld().getRegistryKey();
		String worldId = worldKey.getValue().toString();
		
		Jail jail = new Jail(name, pos, worldId);
		OldSchoolJailMod.getJailData().addJail(jail);
		
		source.sendFeedback(() -> Text.literal("§aJail '" + name + "' set at " + 
			pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), true);
		
		return 1;
	}
	
	private static int deleteJail(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		if (!PermissionUtil.hasPermission(source, PermissionUtil.DELETE_JAIL)) {
			source.sendError(Text.literal("§cYou don't have permission to delete jails!"));
			return 0;
		}
		
		String name = StringArgumentType.getString(context, "name");
		JailData jailData = OldSchoolJailMod.getJailData();
		
		if (!jailData.hasJail(name)) {
			source.sendError(Text.literal("§cJail '" + name + "' doesn't exist!"));
			return 0;
		}
		
		// Release players in this jail
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		for (JailedPlayer jp : jailedData.getPlayersInJail(name)) {
			jailedData.releasePlayer(jp.getPlayerUuid());
			ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(jp.getPlayerUuid());
			if (player != null) {
				player.sendMessage(Text.literal("§aYou have been released because the jail was deleted."));
			}
		}
		
		jailData.removeJail(name);
		source.sendFeedback(() -> Text.literal("§aDeleted jail '" + name + "' and released all prisoners."), true);
		
		return 1;
	}
	
	private static int releasePlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		if (!PermissionUtil.hasPermission(source, PermissionUtil.RELEASE_PLAYER)) {
			source.sendError(Text.literal("§cYou don't have permission to release players!"));
			return 0;
		}
		
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		
		if (!jailedData.isJailed(target.getUuid())) {
			source.sendError(Text.literal("§c" + target.getName().getString() + " is not jailed!"));
			return 0;
		}
		
		jailedData.releasePlayer(target.getUuid());
		target.sendMessage(Text.literal(OldSchoolJailMod.getConfig().releaseMessage));
		source.sendFeedback(() -> Text.literal("§aReleased " + target.getName().getString() + " from jail."), true);
		
		return 1;
	}
	
	private static int checkJailTime(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		ServerPlayerEntity player = source.getPlayerOrThrow();
		
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		JailedPlayer jailed = jailedData.getJailedPlayer(player.getUuid());
		
		if (jailed == null) {
			source.sendError(Text.literal("§cYou are not jailed!"));
			return 0;
		}
		
		long remaining = jailed.getRemainingTimeSeconds();
		source.sendFeedback(() -> Text.literal("§eYou will be released in: " + formatTime(remaining)), false);
		
		return 1;
	}
	
	private static void teleportToJail(ServerPlayerEntity player, Jail jail) {
		// Get the world
		MinecraftServer server = player.getServer();
		if (server == null) return;
		
		RegistryKey<World> worldKey = RegistryKey.of(
			net.minecraft.registry.RegistryKeys.WORLD,
			net.minecraft.util.Identifier.of(jail.getWorldId())
		);
		
		net.minecraft.server.world.ServerWorld world = server.getWorld(worldKey);
		if (world == null) {
			world = server.getOverworld();
		}
		
		BlockPos pos = jail.getPosition();
		player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
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

