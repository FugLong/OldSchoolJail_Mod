package com.oldschooljail.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.oldschooljail.OldSchoolJailMod;
import com.oldschooljail.model.Jail;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class JailData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private final File dataFile;
	private final Map<String, JailEntry> jails = new HashMap<>();
	
	private JailData(File dataFile) {
		this.dataFile = dataFile;
	}
	
	public static JailData load(MinecraftServer server) {
		File worldDir = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT).toFile();
		File dataFile = new File(worldDir, "oldschooljail_jails.json");
		
		JailData data = new JailData(dataFile);
		
		if (dataFile.exists()) {
			try (FileReader reader = new FileReader(dataFile)) {
				Map<String, JailEntry> loadedJails = GSON.fromJson(reader, 
					new TypeToken<Map<String, JailEntry>>(){}.getType());
				if (loadedJails != null) {
					data.jails.putAll(loadedJails);
				}
				OldSchoolJailMod.LOGGER.info("Loaded {} jails", data.jails.size());
			} catch (IOException e) {
				OldSchoolJailMod.LOGGER.error("Failed to load jails data", e);
			}
		}
		
		return data;
	}
	
	public void save() {
		try {
			dataFile.getParentFile().mkdirs();
			try (FileWriter writer = new FileWriter(dataFile)) {
				GSON.toJson(jails, writer);
			}
			OldSchoolJailMod.LOGGER.info("Saved {} jails", jails.size());
		} catch (IOException e) {
			OldSchoolJailMod.LOGGER.error("Failed to save jails data", e);
		}
	}
	
	public void addJail(Jail jail) {
		JailEntry entry = new JailEntry();
		entry.x = jail.getX();
		entry.y = jail.getY();
		entry.z = jail.getZ();
		entry.yaw = jail.getYaw();
		entry.pitch = jail.getPitch();
		entry.worldId = jail.getWorldId();
		jails.put(jail.getName().toLowerCase(), entry);
		save();
	}
	
	public Jail getJail(String name) {
		JailEntry entry = jails.get(name.toLowerCase());
		if (entry == null) return null;
		return new Jail(name, entry.x, entry.y, entry.z, entry.yaw, entry.pitch, entry.worldId);
	}
	
	public boolean hasJail(String name) {
		return jails.containsKey(name.toLowerCase());
	}
	
	public void removeJail(String name) {
		jails.remove(name.toLowerCase());
		save();
	}
	
	public Map<String, JailEntry> getAllJails() {
		return new HashMap<>(jails);
	}
	
	// Inner class for JSON serialization
	public static class JailEntry {
		public double x;
		public double y;
		public double z;
		public float yaw;
		public float pitch;
		public String worldId;
	}
}

