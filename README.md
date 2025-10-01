# Old School Jail Mod

A classic jail mod for Minecraft Fabric servers (1.21.8+) - just like the good old days!

<p align="center">
  <img src="OldSchoolJailLogo.jpeg" alt="Old School Jail Logo" width="400">
</p>

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
- **Auto-Release**: Players are automatically released when their sentence expires and teleported back
- **Original Location Tracking**: Players are teleported back to their original location upon release
- **Logout/Login Handling**: Players remain jailed across logout/login and are auto-released if sentence expired offline
- **Multiple Jails**: Create multiple jails and choose which one to send players to

## Commands

### Main Commands
- `/jail <player> <time> <reason>` - Jail a player in the first available jail
  - Time is based on config setting (default: minutes)
  - Example: `/jail Steve 30 Griefing the spawn`

- `/jail <player> <jail_name> <time> <reason>` - Jail a player in a specific jail
  - Example: `/jail Steve spawn 30 Being annoying`

- `/jail release <player>` - Release a player from jail early and teleport them back to where they were

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

The config file is located at `config/oldschooljail.toml` and is automatically generated on first run with helpful comments.

### Config Options

The TOML file includes detailed comments for each option. Here's what you can configure:

**Time Settings:**
- `input_unit` - Time unit for commands (SECONDS, MINUTES, or HOURS)
- `max_sentence_seconds` - Maximum sentence length in seconds

**Restrictions:**
- `block_commands` - Block all commands except /jail time
- `block_teleportation` - Auto-teleport back if player escapes >50 blocks
- `block_block_breaking` - Prevent breaking blocks
- `block_block_placing` - Prevent placing blocks  
- `block_interaction` - Prevent interacting with blocks

**Messages:**
- `release` - Message on manual release
- `jail_expired` - Message when sentence expires

All settings include helpful comments in the config file explaining what they do!

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
