package com.oldschooljail;

import com.oldschooljail.command.JailCommand;
import com.oldschooljail.config.JailConfig;
import com.oldschooljail.data.JailData;
import com.oldschooljail.data.JailedPlayersData;
import com.oldschooljail.event.PlayerEventHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldSchoolJailMod implements ModInitializer {
	public static final String MOD_ID = "oldschooljail";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	
	private static JailConfig config;
	private static JailData jailData;
	private static JailedPlayersData jailedPlayersData;

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Old School Jail Mod");
		
		// Load config
		config = JailConfig.load();
		
		// Register commands on dedicated server only
		if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
			CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
				JailCommand.register(dispatcher);
			});
		}
		
		// Register player events
		PlayerEventHandler.register();
		
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (jailedPlayersData != null) {
				jailedPlayersData.tick(server);
			}
		});

		// Server lifecycle events
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			jailData = JailData.load(server);
			jailedPlayersData = JailedPlayersData.load(server);
		});
		
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (jailData != null) {
				jailData.save();
			}
			if (jailedPlayersData != null) {
				jailedPlayersData.save();
			}
		});
	}
	
	public static JailConfig getConfig() {
		return config;
	}
	
	public static JailData getJailData() {
		return jailData;
	}
	
	public static JailedPlayersData getJailedPlayersData() {
		return jailedPlayersData;
	}
}

