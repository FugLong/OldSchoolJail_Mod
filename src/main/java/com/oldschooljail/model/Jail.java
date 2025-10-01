package com.oldschooljail.model;

public class Jail {
	private final String name;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final float pitch;
	private final String worldId;
	
	public Jail(String name, double x, double y, double z, float yaw, float pitch, String worldId) {
		this.name = name;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
		this.worldId = worldId;
	}
	
	public String getName() {
		return name;
	}
	
	public double getX() {
		return x;
	}
	
	public double getY() {
		return y;
	}
	
	public double getZ() {
		return z;
	}
	
	public float getYaw() {
		return yaw;
	}
	
	public float getPitch() {
		return pitch;
	}
	
	public String getWorldId() {
		return worldId;
	}
}

