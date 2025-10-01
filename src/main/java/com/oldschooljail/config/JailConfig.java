package com.oldschooljail.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.oldschooljail.OldSchoolJailMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class JailConfig {
	public enum TimeUnit {
		SECONDS,
		MINUTES,
		HOURS
	}
	
	// Default values
	public TimeUnit inputTimeUnit = TimeUnit.MINUTES;
	public long maxSentenceSeconds = 86400; // 24 hours in seconds
	public boolean allowJailTime = true; // Allow /jail time command for jailed players
	public boolean blockCommands = true; // Block all commands except chat and /jail time
	public boolean blockTeleportation = true; // Block teleportation
	public boolean blockBlockBreaking = true; // Block breaking blocks
	public boolean blockBlockPlacing = true; // Block placing blocks
	public boolean blockInteraction = true; // Block interactions (buttons, levers, etc.)
	public String jailMessage = "§cYou have been jailed for %time% by %jailer%!\n§eReason: %reason%";
	public String releaseMessage = "§aYou have been released from jail!";
	public String jailExpiredMessage = "§aYour jail sentence has expired. You are now free!";
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "oldschooljail.json");
	
	public static JailConfig load() {
		if (CONFIG_FILE.exists()) {
			try (FileReader reader = new FileReader(CONFIG_FILE)) {
				JailConfig config = GSON.fromJson(reader, JailConfig.class);
				OldSchoolJailMod.LOGGER.info("Loaded config from " + CONFIG_FILE.getAbsolutePath());
				return config;
			} catch (IOException e) {
				OldSchoolJailMod.LOGGER.error("Failed to load config, using defaults", e);
				return createDefault();
			}
		} else {
			return createDefault();
		}
	}
	
	private static JailConfig createDefault() {
		JailConfig config = new JailConfig();
		config.save();
		OldSchoolJailMod.LOGGER.info("Created default config at " + CONFIG_FILE.getAbsolutePath());
		return config;
	}
	
	public void save() {
		try {
			CONFIG_FILE.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
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

