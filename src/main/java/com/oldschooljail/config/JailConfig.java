package com.oldschooljail.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.oldschooljail.OldSchoolJailMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Path;

public class JailConfig {
	public enum TimeUnit {
		SECONDS,
		MINUTES,
		HOURS
	}
	
	// Default values
	public TimeUnit inputTimeUnit = TimeUnit.MINUTES;
	public long maxSentenceSeconds = 86400; // 24 hours in seconds
	public boolean allowJailTime = true;
	public boolean blockCommands = true;
	public boolean blockTeleportation = true;
	public boolean blockBlockBreaking = true;
	public boolean blockBlockPlacing = true;
	public boolean blockInteraction = true;
	public String releaseMessage = "§aYou have been released from jail!";
	public String jailExpiredMessage = "§aYour jail sentence has expired. You are now free!";
	
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("oldschooljail.toml");
	
	public static JailConfig load() {
		JailConfig config = new JailConfig();
		
		if (CONFIG_PATH.toFile().exists()) {
			try (CommentedFileConfig fileConfig = CommentedFileConfig.of(CONFIG_PATH)) {
				fileConfig.load();
				
				config.inputTimeUnit = TimeUnit.valueOf(fileConfig.getOrElse("time.input_unit", "MINUTES"));
				config.maxSentenceSeconds = fileConfig.getLongOrElse("time.max_sentence_seconds", 86400L);
				
				config.allowJailTime = fileConfig.getOrElse("permissions.allow_jail_time_command", true);
				
				config.blockCommands = fileConfig.getOrElse("restrictions.block_commands", true);
				config.blockTeleportation = fileConfig.getOrElse("restrictions.block_teleportation", true);
				config.blockBlockBreaking = fileConfig.getOrElse("restrictions.block_block_breaking", true);
				config.blockBlockPlacing = fileConfig.getOrElse("restrictions.block_block_placing", true);
				config.blockInteraction = fileConfig.getOrElse("restrictions.block_interaction", true);
				
				config.releaseMessage = fileConfig.getOrElse("messages.release", "§aYou have been released from jail!");
				config.jailExpiredMessage = fileConfig.getOrElse("messages.jail_expired", "§aYour jail sentence has expired. You are now free!");
				
				OldSchoolJailMod.LOGGER.info("Loaded config from " + CONFIG_PATH);
			} catch (Exception e) {
				OldSchoolJailMod.LOGGER.error("Failed to load config, using defaults", e);
			}
		} else {
			config.save();
			OldSchoolJailMod.LOGGER.info("Created default config at " + CONFIG_PATH);
		}
		
		return config;
	}
	
	public void save() {
		try (CommentedFileConfig config = CommentedFileConfig.builder(CONFIG_PATH)
				.sync()
				.preserveInsertionOrder()
				.build()) {
			
			config.load();
			
			// Time settings
			config.setComment("time", 
				" Time-related settings for jail sentences");
			config.set("time.input_unit", inputTimeUnit.name());
			config.setComment("time.input_unit",
				" The time unit used in /jail commands (SECONDS, MINUTES, or HOURS)\n" +
				" Example: If set to MINUTES, '/jail player 30 reason' = 30 minutes");
			
			config.set("time.max_sentence_seconds", maxSentenceSeconds);
			config.setComment("time.max_sentence_seconds",
				" Maximum jail sentence length in seconds (regardless of input_unit)\n" +
				" Default: 86400 (24 hours) | Set to -1 for unlimited");
			
			// Permission settings
			config.setComment("permissions",
				" Permission-related settings");
			config.set("permissions.allow_jail_time_command", allowJailTime);
			config.setComment("permissions.allow_jail_time_command",
				" Allow jailed players to use '/jail time' to check their sentence\n" +
				" Recommended: true (so players know when they'll be released)");
			
			// Restriction settings
			config.setComment("restrictions",
				" What actions are blocked while a player is jailed\n" +
				" Note: Chat is always allowed");
			
			config.set("restrictions.block_commands", blockCommands);
			config.setComment("restrictions.block_commands",
				" Block all commands except '/jail time' (if enabled above)\n" +
				" Recommended: true (prevents /home, /spawn, /tpa to escape)");
			
			config.set("restrictions.block_teleportation", blockTeleportation);
			config.setComment("restrictions.block_teleportation",
				" Teleport player back to jail if they move more than 50 blocks away\n" +
				" Recommended: true (prevents escape via plugins/mods)");
			
			config.set("restrictions.block_block_breaking", blockBlockBreaking);
			config.setComment("restrictions.block_block_breaking",
				" Prevent breaking blocks while jailed\n" +
				" Recommended: true");
			
			config.set("restrictions.block_block_placing", blockBlockPlacing);
			config.setComment("restrictions.block_block_placing",
				" Prevent placing blocks while jailed\n" +
				" Recommended: true");
			
			config.set("restrictions.block_interaction", blockInteraction);
			config.setComment("restrictions.block_interaction",
				" Prevent interacting with blocks (buttons, levers, doors, etc.) while jailed\n" +
				" Recommended: true");
			
			// Message settings
			config.setComment("messages",
				" Customizable messages sent to players\n" +
				" Color codes: §a=green, §c=red, §e=yellow, §7=gray, etc.");
			
			config.set("messages.release", releaseMessage);
			config.setComment("messages.release",
				" Message sent when a player is manually released via '/jail release'");
			
			config.set("messages.jail_expired", jailExpiredMessage);
			config.setComment("messages.jail_expired",
				" Message sent when a player's jail time expires automatically");
			
			config.save();
		} catch (Exception e) {
			OldSchoolJailMod.LOGGER.error("Failed to save config", e);
		}
	}
	
	public long convertToSeconds(long time) {
		return switch (inputTimeUnit) {
			case SECONDS -> time;
			case MINUTES -> time * 60;
			case HOURS -> time * 3600;
		};
	}
	
	public String getTimeUnitName() {
		return switch (inputTimeUnit) {
			case SECONDS -> "second(s)";
			case MINUTES -> "minute(s)";
			case HOURS -> "hour(s)";
		};
	}
}
