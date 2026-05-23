package com.oldschooljail.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.model.Jail;
import com.oldschooljail.model.JailedPlayer;
import com.oldschooljail.util.TeleportUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JailedPlayersData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final File dataFile;
	private final Map<UUID, JailedPlayerEntry> jailedPlayers = new ConcurrentHashMap<>();

	private JailedPlayersData(File dataFile) {
		this.dataFile = dataFile;
	}

	public static JailedPlayersData load(MinecraftServer server) {
		File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
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

	public void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		tickExpiredReleases(server);
		if (OldSchoolJailMod.getConfig().blockTeleportation) {
			tickEscapePrevention(server);
		}
	}

	private void tickExpiredReleases(MinecraftServer server) {
		List<UUID> toRelease = new ArrayList<>();

		for (Map.Entry<UUID, JailedPlayerEntry> entry : jailedPlayers.entrySet()) {
			JailedPlayer jp = toJailedPlayer(entry.getKey(), entry.getValue());
			if (jp.shouldBeReleased()) {
				toRelease.add(entry.getKey());
			}
		}

		for (UUID uuid : toRelease) {
			JailedPlayer jp = getJailedPlayer(uuid);
			if (jp == null) {
				continue;
			}

			ServerPlayer player = server.getPlayerList().getPlayer(uuid);
			if (player != null) {
				player.sendSystemMessage(Component.literal(OldSchoolJailMod.getConfig().jailExpiredMessage));
				if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
					teleportToOriginalLocation(player, jp, server);
				}
				releasePlayer(uuid);
			}
		}
	}

	private void tickEscapePrevention(MinecraftServer server) {
		JailData jailData = OldSchoolJailMod.getJailData();
		if (jailData == null) {
			return;
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (!isJailed(player.getUUID())) {
				continue;
			}

			JailedPlayer jailedPlayer = getJailedPlayer(player.getUUID());
			Jail jail = jailData.getJail(jailedPlayer.getJailName());
			if (jail == null) {
				player.sendSystemMessage(Component.literal("§aYour jail was removed. You are free!"));
				if (OldSchoolJailMod.getConfig().teleportBackOnRelease) {
					teleportToOriginalLocation(player, jailedPlayer, server);
				}
				releasePlayer(player.getUUID());
				continue;
			}

			String playerWorldId = com.oldschooljail.util.PlayerWorldUtil.getWorldId(player);
			boolean wrongDimension = !jail.getWorldId().equals(playerWorldId);
			double distanceSq = player.distanceToSqr(jail.getX(), jail.getY(), jail.getZ());

			if (wrongDimension || distanceSq > 50 * 50) {
				TeleportUtil.teleportToJail(player, jail, server);
				player.sendSystemMessage(Component.literal("§cYou cannot escape from jail!"), true);
			}
		}
	}

	public void jailPlayer(JailedPlayer jailedPlayer) {
		JailedPlayerEntry entry = new JailedPlayerEntry();
		entry.jailName = jailedPlayer.getJailName();
		entry.releaseTime = jailedPlayer.getReleaseTime();
		entry.reason = jailedPlayer.getReason();
		entry.jailedBy = jailedPlayer.getJailedBy();
		entry.originalX = jailedPlayer.getOriginalX();
		entry.originalY = jailedPlayer.getOriginalY();
		entry.originalZ = jailedPlayer.getOriginalZ();
		entry.originalYaw = jailedPlayer.getOriginalYaw();
		entry.originalPitch = jailedPlayer.getOriginalPitch();
		entry.originalWorld = jailedPlayer.getOriginalWorld();
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

	public Collection<JailedPlayer> getAllJailedPlayers() {
		List<JailedPlayer> players = new ArrayList<>();
		for (Map.Entry<UUID, JailedPlayerEntry> entry : jailedPlayers.entrySet()) {
			players.add(toJailedPlayer(entry.getKey(), entry.getValue()));
		}
		return players;
	}

	private JailedPlayer toJailedPlayer(UUID uuid, JailedPlayerEntry entry) {
		return new JailedPlayer(uuid, entry.jailName, entry.releaseTime, entry.reason, entry.jailedBy,
			entry.originalX, entry.originalY, entry.originalZ,
			entry.originalYaw, entry.originalPitch, entry.originalWorld);
	}

	public void teleportToOriginalLocation(ServerPlayer player, JailedPlayer jailedPlayer, MinecraftServer server) {
		TeleportUtil.teleportToSavedLocation(
			player,
			server,
			jailedPlayer.getOriginalX(),
			jailedPlayer.getOriginalY(),
			jailedPlayer.getOriginalZ(),
			jailedPlayer.getOriginalYaw(),
			jailedPlayer.getOriginalPitch(),
			jailedPlayer.getOriginalWorld()
		);
	}

	public static class JailedPlayerEntry {
		public String jailName;
		public long releaseTime;
		public String reason;
		public String jailedBy;
		public double originalX;
		public double originalY;
		public double originalZ;
		public float originalYaw;
		public float originalPitch;
		public String originalWorld;
	}
}
