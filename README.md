<div align="center"><h2>Slo: <ins>S</ins>erver <ins>Lo</ins>ader</h2></div>

With **Slo** installed, Minecraft servers in your `saves` directory will be available during world selection. With the right configuration, you can even choose the server type when creating a world!

### Additional Features

- Open the server console in-game (<code>`</code> by default)
- Autodetect server JAR files, auto-accept EULA
- Seamless integration with Minecraft's UI

## How It Works

When developing a Minecraft server, it's good practice to run it on your machine and connect to it locally. **Slo** automates this process: given a server world, the mod starts the server JAR, listens to its output, and connects to `localhost` when ready. If  anything goes wrong, or if the player disconnects, the server process exits gracefully.

**Slo is a client-side mod**—it integrates local server processes into Minecraft's UI. However, the mod may require you to be connected to the internet; server software usually downloads Minecraft's files on first launch.

## Configuration

**Slo** supports *server presets*—templates for a server save—that users can choose from when creating a world. The mod loads these presets from the configuration directory; alternatively, users can drag and drop presets into the selection screen. To learn how to create these presets, [visit the wiki](https://github.com/acikek/slo/wiki).

## License

MIT © 2025 Skye Prince

> **Note: by running this mod, you agree to [Minecraft's End User License Agreement (EULA).](https://www.minecraft.net/en-us/eula)**
