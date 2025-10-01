package com.oldschooljail.model;

import net.minecraft.util.math.BlockPos;

public class Jail {
	private final String name;
	private final BlockPos position;
	private final String worldId;
	
	public Jail(String name, BlockPos position, String worldId) {
		this.name = name;
		this.position = position;
		this.worldId = worldId;
	}
	
	public String getName() {
		return name;
	}
	
	public BlockPos getPosition() {
		return position;
	}
	
	public String getWorldId() {
		return worldId;
	}
}

