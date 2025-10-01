package com.oldschooljail.model;

import java.util.UUID;

public class JailedPlayer {
	private final UUID playerUuid;
	private final String jailName;
	private final long releaseTime; // Timestamp in milliseconds
	private final String reason;
	private final String jailedBy;
	private final double originalX;
	private final double originalY;
	private final double originalZ;
	private final float originalYaw;
	private final float originalPitch;
	private final String originalWorld;
	
	public JailedPlayer(UUID playerUuid, String jailName, long releaseTime, String reason, String jailedBy,
						double originalX, double originalY, double originalZ, 
						float originalYaw, float originalPitch, String originalWorld) {
		this.playerUuid = playerUuid;
		this.jailName = jailName;
		this.releaseTime = releaseTime;
		this.reason = reason;
		this.jailedBy = jailedBy;
		this.originalX = originalX;
		this.originalY = originalY;
		this.originalZ = originalZ;
		this.originalYaw = originalYaw;
		this.originalPitch = originalPitch;
		this.originalWorld = originalWorld;
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
	
	public double getOriginalX() {
		return originalX;
	}
	
	public double getOriginalY() {
		return originalY;
	}
	
	public double getOriginalZ() {
		return originalZ;
	}
	
	public float getOriginalYaw() {
		return originalYaw;
	}
	
	public float getOriginalPitch() {
		return originalPitch;
	}
	
	public String getOriginalWorld() {
		return originalWorld;
	}
	
	public long getRemainingTimeSeconds() {
		return Math.max(0, (releaseTime - System.currentTimeMillis()) / 1000);
	}
	
	public boolean shouldBeReleased() {
		return System.currentTimeMillis() >= releaseTime;
	}
}

