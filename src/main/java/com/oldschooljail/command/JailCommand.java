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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.Set;
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
						.executes(JailCommand::jailPlayer)))
				// /jail <player> <jail_name> <time> <reason>
				.then(CommandManager.argument("jail_name", StringArgumentType.word())
					.suggests(JAIL_NAME_SUGGESTIONS)
					.then(CommandManager.argument("time", IntegerArgumentType.integer(1))
						.then(CommandManager.argument("reason", StringArgumentType.greedyString())
							.executes(JailCommand::jailPlayerToSpecificJail)))))
			
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
				.executes(JailCommand::checkJailTime)
				// /jail time <player>
				.then(CommandManager.argument("player", EntityArgumentType.player())
					.executes(JailCommand::checkPlayerJailTime)))
			
			// /jail list
			.then(CommandManager.literal("list")
				.executes(JailCommand::listJailedPlayers))
		);
	}
	
	private static int jailPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		int time = IntegerArgumentType.getInteger(context, "time");
		String reason = StringArgumentType.getString(context, "reason");
		
		// Get first available jail
		JailData jailData = OldSchoolJailMod.getJailData();
		Jail jail = jailData.getAllJails().entrySet().stream()
			.findFirst()
			.map(e -> jailData.getJail(e.getKey()))
			.orElse(null);
		
		if (jail == null) {
			context.getSource().sendError(Text.literal("§cNo jails have been set! Use /jail set <name> first."));
			return 0;
		}
		
		return executeJail(context.getSource(), target, time, jail.getName(), reason);
	}
	
	private static int jailPlayerToSpecificJail(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		int time = IntegerArgumentType.getInteger(context, "time");
		String jailName = StringArgumentType.getString(context, "jail_name");
		String reason = StringArgumentType.getString(context, "reason");
		
		// Check if jail exists
		JailData jailData = OldSchoolJailMod.getJailData();
		if (!jailData.hasJail(jailName)) {
			context.getSource().sendError(Text.literal("§cJail '" + jailName + "' doesn't exist!"));
			return 0;
		}
		
		return executeJail(context.getSource(), target, time, jailName, reason);
	}
	
	private static int executeJail(ServerCommandSource source, ServerPlayerEntity target, int time, String jailName, String reason) throws CommandSyntaxException {
		// Check permission
		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendError(Text.literal("§cYou don't have permission to jail players!"));
			return 0;
		}
		
		// Prevent self-jailing
		if (source.getPlayer() != null && source.getPlayer().equals(target)) {
			source.sendError(Text.literal("§cYou can't jail yourself!"));
			return 0;
		}
		
		// Check if target is immune
		if (PermissionUtil.isImmune(target)) {
			source.sendError(Text.literal("§cThat player is immune to jailing!"));
			return 0;
		}
		
		// Check if target is already jailed
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData.isJailed(target.getUuid())) {
			JailedPlayer existingJail = jailedData.getJailedPlayer(target.getUuid());
			long remaining = existingJail.getRemainingTimeSeconds();
			source.sendError(Text.literal("§c" + target.getName().getString() + " is already jailed! " +
				"Remaining time: " + formatTime(remaining) + ". Use /jail release first."));
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
		
		Jail jail = jailData.getJail(jailName);
		if (jail == null) {
			source.sendError(Text.literal("§cJail '" + jailName + "' doesn't exist!"));
			return 0;
		}
		
		// Capture original location
		double origX = target.getX();
		double origY = target.getY();
		double origZ = target.getZ();
		float origYaw = target.getYaw();
		float origPitch = target.getPitch();
		String origWorld = target.getWorld().getRegistryKey().getValue().toString();
		
		// Jail the player
		long releaseTime = System.currentTimeMillis() + (timeInSeconds * 1000);
		String jailerName = source.getName();
		
		JailedPlayer jailedPlayer = new JailedPlayer(
			target.getUuid(),
			jail.getName(),
			releaseTime,
			reason,
			jailerName,
			origX, origY, origZ,
			origYaw, origPitch, origWorld
		);
		
		jailedData.jailPlayer(jailedPlayer);
		
		// Teleport player to jail
		teleportToJail(target, jail);
		
		// Send messages - send as two separate lines for proper formatting
		target.sendMessage(Text.literal("§cYou have been jailed for " + formatTime(timeInSeconds) + " by " + jailerName + "!"));
		target.sendMessage(Text.literal("§eReason: " + reason));
		source.sendFeedback(() -> Text.literal("§aJailed " + target.getName().getString() + 
			" in '" + jail.getName() + "' for " + formatTime(timeInSeconds) + "!"), true);
		
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
		
		// Capture exact position and rotation
		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		float yaw = player.getYaw();
		float pitch = player.getPitch();
		RegistryKey<World> worldKey = player.getWorld().getRegistryKey();
		String worldId = worldKey.getValue().toString();
		
		Jail jail = new Jail(name, x, y, z, yaw, pitch, worldId);
		OldSchoolJailMod.getJailData().addJail(jail);
		
		source.sendFeedback(() -> Text.literal("§aJail '" + name + "' set at " + 
			String.format("%.2f, %.2f, %.2f", x, y, z)), true);
		
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
		
		// When setting by coordinates, use block center and default rotation
		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;
		float yaw = 0;
		float pitch = 0;
		
		RegistryKey<World> worldKey = source.getWorld().getRegistryKey();
		String worldId = worldKey.getValue().toString();
		
		Jail jail = new Jail(name, x, y, z, yaw, pitch, worldId);
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
		
		// Release players in this jail and teleport them back (if enabled)
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		for (JailedPlayer jp : jailedData.getPlayersInJail(name)) {
			ServerPlayerEntity player = source.getServer().getPlayerManager().getPlayer(jp.getPlayerUuid());
			if (player != null) {
				if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
					jailedData.teleportToOriginalLocation(player, jp, source.getServer());
				}
				player.sendMessage(Text.literal("§aYou have been released because the jail was deleted."));
			}
			jailedData.releasePlayer(jp.getPlayerUuid());
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
		
		// Get jailed player data before releasing
		JailedPlayer jailedPlayer = jailedData.getJailedPlayer(target.getUuid());
		
		// Release and teleport back (if enabled)
		jailedData.releasePlayer(target.getUuid());
		
		if (jailedPlayer != null && OldSchoolJailMod.getConfig().teleportBackOnRelease) {
			jailedData.teleportToOriginalLocation(target, jailedPlayer, source.getServer());
		}
		
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
	
	private static int checkPlayerJailTime(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		// Check permission - only admins can check other players' jail time
		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendError(Text.literal("§cYou don't have permission to check other players' jail time!"));
			return 0;
		}
		
		ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		JailedPlayer jailed = jailedData.getJailedPlayer(target.getUuid());
		
		if (jailed == null) {
			source.sendError(Text.literal("§c" + target.getName().getString() + " is not jailed!"));
			return 0;
		}
		
		long remaining = jailed.getRemainingTimeSeconds();
		source.sendFeedback(() -> Text.literal("§e" + target.getName().getString() + " will be released in: " + formatTime(remaining)), false);
		
		return 1;
	}
	
	private static int listJailedPlayers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		
		// Check permission - only admins can list jailed players
		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendError(Text.literal("§cYou don't have permission to list jailed players!"));
			return 0;
		}
		
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		Collection<JailedPlayer> jailedPlayers = jailedData.getAllJailedPlayers();
		
		if (jailedPlayers.isEmpty()) {
			source.sendFeedback(() -> Text.literal("§aNo players are currently jailed."), false);
			return 1;
		}
		
		source.sendFeedback(() -> Text.literal("§e=== Currently Jailed Players ==="), false);
		
		for (JailedPlayer jailedPlayer : jailedPlayers) {
			// Try to get player name from server
			final MinecraftServer server = source.getServer();
			final String playerName;
			
			if (server != null) {
				ServerPlayerEntity player = server.getPlayerManager().getPlayer(jailedPlayer.getPlayerUuid());
				if (player != null) {
					playerName = player.getName().getString();
				} else {
					// Player is offline, use UUID as fallback
					playerName = jailedPlayer.getPlayerUuid().toString().substring(0, 8) + "...";
				}
			} else {
				playerName = "Unknown";
			}
			
			final long remaining = jailedPlayer.getRemainingTimeSeconds();
			final String status = server != null && server.getPlayerManager().getPlayer(jailedPlayer.getPlayerUuid()) != null ? "§aOnline" : "§7Offline";
			
			source.sendFeedback(() -> Text.literal("§e" + playerName + " §7- Jail: §f" + jailedPlayer.getJailName() + 
				" §7- Time: §f" + formatTime(remaining) + " §7- Status: " + status), false);
			source.sendFeedback(() -> Text.literal("§7  Reason: §f" + jailedPlayer.getReason() + 
				" §7- Jailed by: §f" + jailedPlayer.getJailedBy()), false);
		}
		
		source.sendFeedback(() -> Text.literal("§eTotal: §f" + jailedPlayers.size() + " §eplayers jailed"), false);
		
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
		
		// Use exact position and rotation from jail
		player.teleport(world, jail.getX(), jail.getY(), jail.getZ(), Set.of(), jail.getYaw(), jail.getPitch(), true);
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

