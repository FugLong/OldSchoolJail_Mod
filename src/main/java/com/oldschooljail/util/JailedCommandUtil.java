package com.oldschooljail.util;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;

/**
 * Determines whether a parsed command is the jailed-player-allowed {@code /jail time} (no target argument).
 */
public final class JailedCommandUtil {

	private JailedCommandUtil() {
	}

	public static boolean isSelfJailTimeCommand(ParseResults<CommandSourceStack> parseResults) {
		boolean sawJail = false;
		boolean sawTime = false;
		boolean sawOtherPlayerArg = false;

		for (var node : parseResults.getContext().getNodes()) {
			CommandNode<CommandSourceStack> commandNode = node.getNode();
			String name = commandNode.getName();

			if ("jail".equals(name)) {
				sawJail = true;
			} else if ("time".equals(name) && sawJail) {
				sawTime = true;
			} else if ("player".equals(name) && commandNode instanceof ArgumentCommandNode<?, ?> && sawTime) {
				sawOtherPlayerArg = true;
			}
		}

		return sawJail && sawTime && !sawOtherPlayerArg;
	}

	public static boolean canJailedPlayerUseJailTime(CommandSourceStack source) {
		if (!com.oldschooljail.OldSchoolJailMod.getConfig().allowJailTime) {
			return false;
		}
		return PermissionUtil.hasJailTimePermission(source);
	}
}
