# Old School Jail Mod

A classic jail mod for Minecraft Fabric servers (1.21.8+) - just like the good old days!

## Features

- **Jail Players**: Jail players with customizable time limits and reasons
- **Jail Management**: Set multiple jail locations and delete them as needed
- **Permissions System**: Full integration with Fabric Permissions API (works with LuckPerms and other permission mods)
- **Smart Restrictions**: Jailed players cannot:
  - Break or place blocks
  - Interact with blocks (buttons, levers, etc.)
  - Use commands (except `/jail time` to check their sentence)
  - Teleport or escape (automatic teleport back to jail)
- **Configurable**: Extensive config file for customization
- **Data Persistence**: All jails and jailed players are saved and restored on server restart
- **Auto-Release**: Players are automatically released when their sentence expires

## Commands

### Main Commands
- `/jail <player> <time> <reason>` - Jail a player
  - Time is based on config setting (default: minutes)
  - Example: `/jail Steve 30 Griefing the spawn`

- `/jail release <player>` - Release a player from jail early

- `/jail time` - Check remaining jail time (available to jailed players)

### Admin Commands
- `/jail set <jail_name>` - Set a jail at your current location
- `/jail set <jail_name> <x> <y> <z>` - Set a jail at specific coordinates
- `/jail delete <jail_name>` - Delete a jail (automatically releases all prisoners)

## Permissions

The mod uses the Fabric Permissions API and supports any permissions plugin that implements it (LuckPerms, etc.)

### Permission Nodes
- `oldschooljail.jail` - Allows jailing players
- `oldschooljail.release` - Allows releasing players
- `oldschooljail.set` - Allows setting jail locations
- `oldschooljail.delete` - Allows deleting jails
- `oldschooljail.immune` - Makes a player immune to being jailed
- `oldschooljail.time` - Allows using `/jail time` (granted to all by default)

**Note**: If no permissions plugin is installed, the mod falls back to OP level 2 for admin commands.

## Configuration

The config file is located at `config/oldschooljail.json` and is automatically generated on first run.

### Config Options
```json
{
  "inputTimeUnit": "MINUTES",           // Time unit for jail commands: SECONDS, MINUTES, or HOURS
  "maxSentenceSeconds": 86400,          // Maximum jail sentence in seconds (24 hours default)
  "allowJailTime": true,                // Allow jailed players to use /jail time
  "blockCommands": true,                // Block all commands except chat and /jail time
  "blockTeleportation": true,           // Block teleportation and escape attempts
  "blockBlockBreaking": true,           // Prevent breaking blocks
  "blockBlockPlacing": true,            // Prevent placing blocks
  "blockInteraction": true,             // Prevent block interactions
  "jailMessage": "§cYou have been jailed for %time% by %jailer%! Reason: %reason%",
  "releaseMessage": "§aYou have been released from jail!",
  "jailExpiredMessage": "§aYour jail sentence has expired. You are now free!"
}
```

## Installation

1. Download the latest release from the [Releases](https://github.com/yourusername/OldSchoolJail_Mod/releases) page
2. Place the JAR file in your server's `mods` folder
3. Ensure you have Fabric API installed
4. (Optional) Install a permissions plugin like LuckPerms for advanced permission control
5. Start your server

## Building from Source

```bash
git clone https://github.com/yourusername/OldSchoolJail_Mod.git
cd OldSchoolJail_Mod
./gradlew build
```

The built JAR will be in `build/libs/`

## Compatibility

- **Minecraft Version**: 1.21.8+
- **Mod Loader**: Fabric
- **Required**: Fabric API
- **Recommended**: LuckPerms or other Fabric Permissions API compatible plugin

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
A jail mod for Fabric servers, just like the good old days!
