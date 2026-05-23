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
import com.oldschooljail.util.PlayerWorldUtil;
import com.oldschooljail.util.TeleportUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Collection;

public class JailCommand {

	private static final SuggestionProvider<CommandSourceStack> JAIL_NAME_SUGGESTIONS = (context, builder) -> {
		JailData jailData = OldSchoolJailMod.getJailData();
		if (jailData != null) {
			return SharedSuggestionProvider.suggest(
				jailData.getAllJails().keySet(),
				builder
			);
		}
		return builder.buildFuture();
	};

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("jail")
			.then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("time", IntegerArgumentType.integer(1))
					.then(Commands.argument("reason", StringArgumentType.greedyString())
						.executes(JailCommand::jailPlayer)))
				.then(Commands.argument("jail_name", StringArgumentType.word())
					.suggests(JAIL_NAME_SUGGESTIONS)
					.then(Commands.argument("time", IntegerArgumentType.integer(1))
						.then(Commands.argument("reason", StringArgumentType.greedyString())
							.executes(JailCommand::jailPlayerToSpecificJail)))))

			.then(Commands.literal("set")
				.then(Commands.argument("name", StringArgumentType.word())
					.executes(JailCommand::setJailAtCurrentPos)
					.then(Commands.argument("pos", BlockPosArgument.blockPos())
						.executes(JailCommand::setJailAtPos))))

			.then(Commands.literal("delete")
				.then(Commands.argument("name", StringArgumentType.word())
					.suggests(JAIL_NAME_SUGGESTIONS)
					.executes(JailCommand::deleteJail)))

			.then(Commands.literal("release")
				.then(Commands.argument("player", EntityArgument.player())
					.executes(JailCommand::releasePlayer)))

			.then(Commands.literal("time")
				.executes(JailCommand::checkJailTime)
				.then(Commands.argument("player", EntityArgument.player())
					.executes(JailCommand::checkPlayerJailTime)))

			.then(Commands.literal("list")
				.executes(JailCommand::listJailedPlayers))
		);
	}

	private static int jailPlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		int time = IntegerArgumentType.getInteger(context, "time");
		String reason = StringArgumentType.getString(context, "reason");

		JailData jailData = requireJailData(context);
		if (jailData == null) {
			return 0;
		}

		Jail jail = jailData.getFirstJail().orElse(null);
		if (jail == null) {
			context.getSource().sendFailure(Component.literal("§cNo jails have been set! Use /jail set <name> first."));
			return 0;
		}

		return executeJail(context.getSource(), target, time, jail.getName(), reason);
	}

	private static int jailPlayerToSpecificJail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		int time = IntegerArgumentType.getInteger(context, "time");
		String jailName = StringArgumentType.getString(context, "jail_name");
		String reason = StringArgumentType.getString(context, "reason");

		JailData jailData = requireJailData(context);
		if (jailData == null) {
			return 0;
		}
		if (!jailData.hasJail(jailName)) {
			context.getSource().sendFailure(Component.literal("§cJail '" + jailName + "' doesn't exist!"));
			return 0;
		}

		return executeJail(context.getSource(), target, time, jailName, reason);
	}

	private static int executeJail(CommandSourceStack source, ServerPlayer target, int time, String jailName, String reason) throws CommandSyntaxException {
		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendFailure(Component.literal("§cYou don't have permission to jail players!"));
			return 0;
		}

		if (source.getPlayer() != null && source.getPlayer().equals(target)) {
			source.sendFailure(Component.literal("§cYou can't jail yourself!"));
			return 0;
		}

		if (PermissionUtil.isImmune(target)) {
			source.sendFailure(Component.literal("§cThat player is immune to jailing!"));
			return 0;
		}

		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData.isJailed(target.getUUID())) {
			JailedPlayer existingJail = jailedData.getJailedPlayer(target.getUUID());
			long remaining = existingJail.getRemainingTimeSeconds();
			source.sendFailure(Component.literal("§c" + target.getGameProfile().name() + " is already jailed! " +
				"Remaining time: " + formatTime(remaining) + ". Use /jail release first."));
			return 0;
		}

		JailConfig config = OldSchoolJailMod.getConfig();
		long timeInSeconds = config.convertToSeconds(time);

		if (config.hasMaxSentenceLimit() && timeInSeconds > config.maxSentenceSeconds) {
			source.sendFailure(Component.literal("§cJail time exceeds maximum allowed sentence of " +
				formatTime(config.maxSentenceSeconds) + "!"));
			return 0;
		}

		JailData jailData = OldSchoolJailMod.getJailData();
		if (jailData == null) {
			source.sendFailure(Component.literal("§cJail data is not loaded yet. Try again in a moment."));
			return 0;
		}

		Jail jail = jailData.getJail(jailName);
		if (jail == null) {
			source.sendFailure(Component.literal("§cJail '" + jailName + "' doesn't exist!"));
			return 0;
		}

		double origX = target.getX();
		double origY = target.getY();
		double origZ = target.getZ();
		float origYaw = target.getYRot();
		float origPitch = target.getXRot();
		String origWorld = PlayerWorldUtil.getWorldId(target);

		long releaseTime = System.currentTimeMillis() + (timeInSeconds * 1000);
		String jailerName = source.getTextName();

		JailedPlayer jailedPlayer = new JailedPlayer(
			target.getUUID(),
			jail.getName(),
			releaseTime,
			reason,
			jailerName,
			origX, origY, origZ,
			origYaw, origPitch, origWorld
		);

		jailedData.jailPlayer(jailedPlayer);

		TeleportUtil.teleportToJail(target, jail, source.getServer());

		target.sendSystemMessage(Component.literal("§cYou have been jailed for " + formatTime(timeInSeconds) + " by " + jailerName + "!"));
		target.sendSystemMessage(Component.literal("§eReason: " + reason));
		source.sendSuccess(() -> Component.literal("§aJailed " + target.getGameProfile().name() +
			" in '" + jail.getName() + "' for " + formatTime(timeInSeconds) + "!"), true);

		return 1;
	}

	private static int setJailAtCurrentPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.SET_JAIL)) {
			source.sendFailure(Component.literal("§cYou don't have permission to set jails!"));
			return 0;
		}

		ServerPlayer player = source.getPlayerOrException();
		String name = StringArgumentType.getString(context, "name");

		double x = player.getX();
		double y = player.getY();
		double z = player.getZ();
		float yaw = player.getYRot();
		float pitch = player.getXRot();
		ResourceKey<Level> levelKey = PlayerWorldUtil.getLevel(player).dimension();
		String worldId = levelKey.identifier().toString();

		JailData jailData = requireJailData(context);
		if (jailData == null) {
			return 0;
		}

		Jail jail = new Jail(name, x, y, z, yaw, pitch, worldId);
		jailData.addJail(jail);

		source.sendSuccess(() -> Component.literal("§aJail '" + name + "' set at " +
			String.format("%.2f, %.2f, %.2f", x, y, z)), true);

		return 1;
	}

	private static int setJailAtPos(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.SET_JAIL)) {
			source.sendFailure(Component.literal("§cYou don't have permission to set jails!"));
			return 0;
		}

		String name = StringArgumentType.getString(context, "name");
		BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

		double x = pos.getX() + 0.5;
		double y = pos.getY();
		double z = pos.getZ() + 0.5;
		float yaw = 0;
		float pitch = 0;

		ResourceKey<Level> levelKey = source.getLevel().dimension();
		String worldId = levelKey.identifier().toString();

		JailData jailData = requireJailData(context);
		if (jailData == null) {
			return 0;
		}

		Jail jail = new Jail(name, x, y, z, yaw, pitch, worldId);
		jailData.addJail(jail);

		source.sendSuccess(() -> Component.literal("§aJail '" + name + "' set at " +
			pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), true);

		return 1;
	}

	private static int deleteJail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.DELETE_JAIL)) {
			source.sendFailure(Component.literal("§cYou don't have permission to delete jails!"));
			return 0;
		}

		String name = StringArgumentType.getString(context, "name");
		JailData jailData = requireJailData(context);
		if (jailData == null) {
			return 0;
		}

		if (!jailData.hasJail(name)) {
			source.sendFailure(Component.literal("§cJail '" + name + "' doesn't exist!"));
			return 0;
		}

		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData == null) {
			source.sendFailure(Component.literal("§cJail data is not loaded yet. Try again in a moment."));
			return 0;
		}
		for (JailedPlayer jp : jailedData.getPlayersInJail(name)) {
			ServerPlayer player = source.getServer().getPlayerList().getPlayer(jp.getPlayerUuid());
			if (player != null) {
				if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
					jailedData.teleportToOriginalLocation(player, jp, source.getServer());
				}
				player.sendSystemMessage(Component.literal("§aYou have been released because the jail was deleted."));
			}
			jailedData.releasePlayer(jp.getPlayerUuid());
		}

		jailData.removeJail(name);
		source.sendSuccess(() -> Component.literal("§aDeleted jail '" + name + "' and released all prisoners."), true);

		return 1;
	}

	private static int releasePlayer(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.RELEASE_PLAYER)) {
			source.sendFailure(Component.literal("§cYou don't have permission to release players!"));
			return 0;
		}

		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();

		if (!jailedData.isJailed(target.getUUID())) {
			source.sendFailure(Component.literal("§c" + target.getGameProfile().name() + " is not jailed!"));
			return 0;
		}

		JailedPlayer jailedPlayer = jailedData.getJailedPlayer(target.getUUID());

		jailedData.releasePlayer(target.getUUID());

		if (jailedPlayer != null && OldSchoolJailMod.getConfig().teleportBackOnRelease) {
			jailedData.teleportToOriginalLocation(target, jailedPlayer, source.getServer());
		}

		target.sendSystemMessage(Component.literal(OldSchoolJailMod.getConfig().releaseMessage));
		source.sendSuccess(() -> Component.literal("§aReleased " + target.getGameProfile().name() + " from jail."), true);

		return 1;
	}

	private static int checkJailTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();

		if (!OldSchoolJailMod.getConfig().allowJailTime) {
			source.sendFailure(Component.literal("§cChecking jail time is disabled on this server."));
			return 0;
		}
		if (!PermissionUtil.hasJailTimePermission(source)) {
			source.sendFailure(Component.literal("§cYou don't have permission to use /jail time!"));
			return 0;
		}

		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		if (jailedData == null) {
			source.sendFailure(Component.literal("§cJail data is not loaded yet. Try again in a moment."));
			return 0;
		}
		JailedPlayer jailed = jailedData.getJailedPlayer(player.getUUID());

		if (jailed == null) {
			source.sendFailure(Component.literal("§cYou are not jailed!"));
			return 0;
		}

		long remaining = jailed.getRemainingTimeSeconds();
		source.sendSuccess(() -> Component.literal("§eYou will be released in: " + formatTime(remaining)), false);

		return 1;
	}

	private static int checkPlayerJailTime(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendFailure(Component.literal("§cYou don't have permission to check other players' jail time!"));
			return 0;
		}

		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		JailedPlayer jailed = jailedData.getJailedPlayer(target.getUUID());

		if (jailed == null) {
			source.sendFailure(Component.literal("§c" + target.getGameProfile().name() + " is not jailed!"));
			return 0;
		}

		long remaining = jailed.getRemainingTimeSeconds();
		source.sendSuccess(() -> Component.literal("§e" + target.getGameProfile().name() + " will be released in: " + formatTime(remaining)), false);

		return 1;
	}

	private static int listJailedPlayers(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		if (!PermissionUtil.hasPermission(source, PermissionUtil.JAIL_PLAYER)) {
			source.sendFailure(Component.literal("§cYou don't have permission to list jailed players!"));
			return 0;
		}

		JailedPlayersData jailedData = OldSchoolJailMod.getJailedPlayersData();
		Collection<JailedPlayer> jailedPlayers = jailedData.getAllJailedPlayers();

		if (jailedPlayers.isEmpty()) {
			source.sendSuccess(() -> Component.literal("§aNo players are currently jailed."), false);
			return 1;
		}

		source.sendSuccess(() -> Component.literal("§e=== Currently Jailed Players ==="), false);

		for (JailedPlayer jailedPlayer : jailedPlayers) {
			final MinecraftServer server = source.getServer();
			final String playerName;

			if (server != null) {
				ServerPlayer player = server.getPlayerList().getPlayer(jailedPlayer.getPlayerUuid());
				if (player != null) {
					playerName = player.getGameProfile().name();
				} else {
					playerName = jailedPlayer.getPlayerUuid().toString().substring(0, 8) + "...";
				}
			} else {
				playerName = "Unknown";
			}

			final long remaining = jailedPlayer.getRemainingTimeSeconds();
			final String status = server != null && server.getPlayerList().getPlayer(jailedPlayer.getPlayerUuid()) != null ? "§aOnline" : "§7Offline";

			source.sendSuccess(() -> Component.literal("§e" + playerName + " §7- Jail: §f" + jailedPlayer.getJailName() +
				" §7- Time: §f" + formatTime(remaining) + " §7- Status: " + status), false);
			source.sendSuccess(() -> Component.literal("§7  Reason: §f" + jailedPlayer.getReason() +
				" §7- Jailed by: §f" + jailedPlayer.getJailedBy()), false);
		}

		source.sendSuccess(() -> Component.literal("§eTotal: §f" + jailedPlayers.size() + " §eplayers jailed"), false);

		return 1;
	}

	private static JailData requireJailData(CommandContext<CommandSourceStack> context) {
		JailData jailData = OldSchoolJailMod.getJailData();
		if (jailData == null) {
			context.getSource().sendFailure(Component.literal("§cJail data is not loaded yet. Try again in a moment."));
		}
		return jailData;
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
