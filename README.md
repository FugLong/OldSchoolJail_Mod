# Old School Jail Mod

A classic jail mod for Minecraft Fabric servers - just like the good old days!

<p align="center">
  <img src="OldSchoolJailLogo.jpeg" alt="Old School Jail Logo" width="400">
</p>

## Features

- **Jail Players**: Jail players with customizable time limits and reasons
- **Jail Management**: Set multiple jail locations and delete them as needed
- **Permissions System**: Integrates with the [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) (LuckPerms and other compatible plugins)
- **Smart Restrictions**: Jailed players cannot:
  - Break or place blocks
  - Interact with blocks (buttons, levers, etc.)
  - Use commands (except `/jail time` to check their own sentence, when allowed)
  - Teleport or escape (automatic teleport back to jail; pearls/chorus/wind charges blocked when escape prevention is on)
- **Configurable**: Extensive config file for customization
- **Data Persistence**: All jails and jailed players are saved and restored on server restart
- **Auto-Release**: Players are automatically released when their sentence expires
- **Original Location Tracking**: Players are teleported back to their original location upon release (configurable)
- **Logout/Login Handling**: Players remain jailed across logout/login and are auto-released if their sentence expired offline
- **Multiple Jails**: Create multiple jails and choose which one to send players to
- **Works alongside** [Essential Commands](https://modrinth.com/mod/essential-commands) and other command mods (jailed players have commands blocked by default)

## Commands

### Main Commands
- `/jail <player> <time> <reason>` - Jail a player in the **oldest** jail (first one created)
  - Time is based on config setting (default: minutes)
  - Example: `/jail Steve 30 Griefing the spawn`

- `/jail <player> <jail_name> <time> <reason>` - Jail a player in a specific jail
  - Example: `/jail Steve spawn 30 Being annoying`

- `/jail release <player>` - Release a player from jail early (teleports back to original location if enabled in config)

- `/jail time` - Check your own remaining jail time (jailed players only; see permissions/config below)
- `/jail time <player>` - Check any player's jail time (requires `oldschooljail.jail`)
- `/jail list` - List all currently jailed players (requires `oldschooljail.jail`)

### Admin Commands
- `/jail set <jail_name>` - Set a jail at your current location
- `/jail set <jail_name> <x> <y> <z>` - Set a jail at specific coordinates
- `/jail delete <jail_name>` - Delete a jail (automatically releases all prisoners)

## Permissions

Uses the Fabric Permissions API when a compatible permissions plugin is installed (e.g. LuckPerms). The mod detects the API on the classpath automatically.

### Permission Nodes
| Node | Description |
|------|-------------|
| `oldschooljail.jail` | Jail players, list jailed players, check others' jail time |
| `oldschooljail.release` | Release players |
| `oldschooljail.set` | Set jail locations |
| `oldschooljail.delete` | Delete jails |
| `oldschooljail.immune` | Cannot be jailed |
| `oldschooljail.time` | Use `/jail time` while jailed (grant to everyone via LuckPerms default group if desired) |

**Without a permissions plugin:** server operators can run admin commands. Operators are also treated as **immune** to jailing so admins cannot accidentally lock each other out. Non-ops can always use `/jail time` on themselves while jailed unless you disable it in config.

## Configuration

The config file is at `config/oldschooljail.toml` and is generated on first run with comments.

### Config Options

**Time Settings:**
- `time.input_unit` - `SECONDS`, `MINUTES`, or `HOURS` for `/jail` commands
- `time.max_sentence_seconds` - Maximum sentence in seconds (`-1` = unlimited)

**Permissions:**
- `permissions.allow_jail_time_command` - If `false`, jailed players cannot use `/jail time` (command blocking still applies)

**Restrictions:**
- `restrictions.block_commands` - Block all commands except allowed `/jail time`
- `restrictions.block_teleportation` - Teleport back if player leaves jail (>50 blocks) **and** block ender pearls, chorus fruit, and wind charges
- `restrictions.block_block_breaking` - Prevent breaking blocks
- `restrictions.block_block_placing` - Prevent placing blocks
- `restrictions.block_interaction` - Prevent interacting with blocks

**Release:**
- `release.teleport_back_on_release` - Return players to their pre-jail location when released

**Messages:**
- `messages.release` - Manual release message
- `messages.jail_expired` - Auto-release message

## Installation

1. Download `oldschooljail-[version].jar` from [Releases](https://github.com/FugLong/OldSchoolJail_Mod/releases)
2. Place it in your server's `mods` folder
3. Install **Fabric API** for your Minecraft version
4. Use **Java 25+**
5. (Optional) LuckPerms or another Fabric Permissions API plugin
6. (Optional) [Essential Commands](https://modrinth.com/mod/essential-commands) or similar
7. Start the server

## Building from Source

Requires **Java 25**. Gradle will auto-download a JDK 25 toolchain if needed.

```bash
git clone https://github.com/FugLong/OldSchoolJail_Mod.git
cd OldSchoolJail_Mod
./gradlew build
```

Output: `build/libs/oldschooljail-<version>.jar`

## Compatibility

| | |
|---|---|
| **Minecraft** | 26.1 – 26.1.2 (one JAR, built on 26.1.2) |
| **Loader** | Fabric |
| **Java** | 25+ |
| **Required** | Fabric API |
| **Optional** | LuckPerms (or any mod providing Fabric Permissions API) |

**Not compatible** with 1.21.x or 26.2+.

For **Minecraft 1.21.2–1.21.11**, use the [`1.21.X`](https://github.com/FugLong/OldSchoolJail_Mod/tree/1.21.X) branch.

## License

MIT — see [LICENSE](LICENSE).

## Contributing

Pull requests welcome.
