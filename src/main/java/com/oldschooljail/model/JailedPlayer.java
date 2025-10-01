package com.oldschooljail.model;

import java.util.UUID;

public class JailedPlayer {
	private final UUID playerUuid;
	private final String jailName;
	private final long releaseTime; // Timestamp in milliseconds
	private final String reason;
	private final String jailedBy;
	
	public JailedPlayer(UUID playerUuid, String jailName, long releaseTime, String reason, String jailedBy) {
		this.playerUuid = playerUuid;
		this.jailName = jailName;
		this.releaseTime = releaseTime;
		this.reason = reason;
		this.jailedBy = jailedBy;
	}
	
	public UUID getPlayerUuid() {
		return playerUuid;
	}
	
	public String getJailName() {
		return jailName;
	}
	
	public long getReleaseTime() {
		return releaseTime;
	}
	
	public String getReason() {
		return reason;
	}
	
	public String getJailedBy() {
		return jailedBy;
	}
	
	public long getRemainingTimeSeconds() {
		return Math.max(0, (releaseTime - System.currentTimeMillis()) / 1000);
	}
	
	public boolean shouldBeReleased() {
		return System.currentTimeMillis() >= releaseTime;
	}
}

