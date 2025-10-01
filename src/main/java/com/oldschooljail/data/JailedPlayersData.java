package com.oldschooljail.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.model.JailedPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class JailedPlayersData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final File dataFile;
	private final Map<UUID, JailedPlayerEntry> jailedPlayers = new HashMap<>();
	private ScheduledExecutorService releaseTimer;
	
	private JailedPlayersData(File dataFile) {
		this.dataFile = dataFile;
	}
	
	public static JailedPlayersData load(MinecraftServer server) {
		File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
		File dataFile = new File(worldDir, "oldschooljail_players.json");
		
		JailedPlayersData data = new JailedPlayersData(dataFile);
		
		if (dataFile.exists()) {
			try (FileReader reader = new FileReader(dataFile)) {
				Map<UUID, JailedPlayerEntry> loaded = GSON.fromJson(reader, 
					new TypeToken<Map<UUID, JailedPlayerEntry>>(){}.getType());
				if (loaded != null) {
					data.jailedPlayers.putAll(loaded);
				}
				OldSchoolJailMod.LOGGER.info("Loaded {} jailed players", data.jailedPlayers.size());
			} catch (IOException e) {
				OldSchoolJailMod.LOGGER.error("Failed to load jailed players data", e);
			}
		}
		
		return data;
	}
	
	public void save() {
		try {
			dataFile.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(dataFile)) {
				GSON.toJson(jailedPlayers, writer);
			}
		} catch (IOException e) {
			OldSchoolJailMod.LOGGER.error("Failed to save jailed players data", e);
		}
	}
	
	public void startReleaseTimer(MinecraftServer server) {
		releaseTimer = Executors.newSingleThreadScheduledExecutor();
		releaseTimer.scheduleAtFixedRate(() -> {
			List<UUID> toRelease = new ArrayList<>();
			
			for (Map.Entry<UUID, JailedPlayerEntry> entry : jailedPlayers.entrySet()) {
				JailedPlayer jp = toJailedPlayer(entry.getKey(), entry.getValue());
				if (jp.shouldBeReleased()) {
					toRelease.add(entry.getKey());
				}
			}
			
			for (UUID uuid : toRelease) {
				server.execute(() -> {
					ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
					if (player != null) {
						player.sendMessage(Text.literal(OldSchoolJailMod.getConfig().jailExpiredMessage));
					}
					releasePlayer(uuid);
				});
			}
		}, 1, 1, TimeUnit.SECONDS);
	}
	
	public void jailPlayer(JailedPlayer jailedPlayer) {
		JailedPlayerEntry entry = new JailedPlayerEntry();
		entry.jailName = jailedPlayer.getJailName();
		entry.releaseTime = jailedPlayer.getReleaseTime();
		entry.reason = jailedPlayer.getReason();
		entry.jailedBy = jailedPlayer.getJailedBy();
		jailedPlayers.put(jailedPlayer.getPlayerUuid(), entry);
		save();
	}
	
	public JailedPlayer getJailedPlayer(UUID uuid) {
		JailedPlayerEntry entry = jailedPlayers.get(uuid);
		if (entry == null) return null;
		return toJailedPlayer(uuid, entry);
	}
	
	public boolean isJailed(UUID uuid) {
		return jailedPlayers.containsKey(uuid);
	}
	
	public void releasePlayer(UUID uuid) {
		jailedPlayers.remove(uuid);
		save();
	}
	
	public List<JailedPlayer> getPlayersInJail(String jailName) {
		List<JailedPlayer> players = new ArrayList<>();
		for (Map.Entry<UUID, JailedPlayerEntry> entry : jailedPlayers.entrySet()) {
			if (entry.getValue().jailName.equalsIgnoreCase(jailName)) {
				players.add(toJailedPlayer(entry.getKey(), entry.getValue()));
			}
		}
		return players;
	}
	
	private JailedPlayer toJailedPlayer(UUID uuid, JailedPlayerEntry entry) {
		return new JailedPlayer(uuid, entry.jailName, entry.releaseTime, entry.reason, entry.jailedBy);
	}
	
	public void shutdown() {
		if (releaseTimer != null) {
			releaseTimer.shutdown();
		}
	}
	
	// Inner class for JSON serialization
	public static class JailedPlayerEntry {
		public String jailName;
		public long releaseTime;
		public String reason;
		public String jailedBy;
	}
}

