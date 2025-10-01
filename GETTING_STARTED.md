# Getting Started with Old School Jail Mod

## Testing the Mod Locally

1. **Build the mod:**
   ```bash
   ./gradlew build
   ```
   The built JAR will be in `build/libs/oldschooljail-1.0.0.jar`

2. **Test in a development environment:**
   ```bash
   ./gradlew runServer
   ```

3. **Install on a server:**
   - Copy the JAR from `build/libs/` to your server's `mods` folder
   - Ensure Fabric API is installed
   - Start the server

## Setting Up Permissions with LuckPerms

1. **Install LuckPerms** on your server

2. **Grant permissions to staff:**
   ```
   /lp group admin permission set oldschooljail.jail true
   /lp group admin permission set oldschooljail.release true
   /lp group admin permission set oldschooljail.set true
   /lp group admin permission set oldschooljail.delete true
   ```

3. **Make VIPs immune to jailing:**
   ```
   /lp group vip permission set oldschooljail.immune true
   ```

## Creating Your First Jail

1. **Go to a jail location** and run:
   ```
   /jail set main
   ```

2. **Jail a player:**
   ```
   /jail PlayerName 30 Breaking the rules
   ```
   (30 minutes by default, configurable in config)

3. **Check remaining time** (as jailed player):
   ```
   /jail time
   ```

4. **Release early:**
   ```
   /jail release PlayerName
   ```

## Creating a GitHub Release

1. **Tag a version:**
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. **GitHub Actions will automatically:**
   - Build the mod
   - Create a release
   - Upload the JAR file

3. **Download from the Releases page** and share!

## Publishing to Modrinth

1. Go to [Modrinth](https://modrinth.com/) and create a new project
2. Fill in the details:
   - **Name:** Old School Jail
   - **Summary:** A classic jail mod for Fabric servers, just like the good old days!
   - **Categories:** Server utility, Administration
   - **License:** MIT
   - **Supported versions:** 1.21.8
   - **Mod loader:** Fabric

3. Upload the JAR from GitHub releases or `build/libs/`
4. Add dependencies: Fabric API (required)
5. Publish!

## Configuration Tips

Edit `config/oldschooljail.json`:

- **Change time units:** Set `inputTimeUnit` to `SECONDS`, `MINUTES`, or `HOURS`
- **Increase max sentence:** Adjust `maxSentenceSeconds` (in seconds)
- **Customize messages:** Edit `jailMessage`, `releaseMessage`, etc.
- **Toggle restrictions:** Set any `block*` options to `false` to disable

## Support

- Report bugs on [GitHub Issues](https://github.com/FugLong/OldSchoolJail_Mod/issues)
- Read the full documentation in [README.md](README.md)

Happy jailing! 🔒

