package com.acikek.slo;

import com.acikek.slo.screen.LoadServerLevelScreen;
import com.acikek.slo.screen.SelectJarCandidateScreen;
import com.acikek.slo.screen.ServerConsoleScreen;
import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Slo implements ClientModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static final Component GUI_START_SERVER_FAIL = Component.translatable("gui.slo.startServerFail");
	public static final Component GUI_START_SERVER_FAIL_INFO = Component.translatable("gui.slo.startServerFail.info");
	public static final Component GUI_STOP_SERVER = Component.translatable("gui.slo.status.stopServer");
	public static final Component GUI_RETRY = Component.translatable("gui.slo.disconnect.retry");
	public static final Component GUI_TO_WORLD = Component.translatable("gui.toWorld");

	public static KeyMapping consoleKey;

	public static Map<String, ExtendedLevelDirectory> worldPresets = new HashMap<>();

	public static boolean directoryInitUpdate = true;
	public static boolean directoryInitAutodetect = true;

	public static ExtendedLevelDirectory createFromExisting;

	public static Process serverProcess;
	public static ExtendedLevelDirectory levelDirectory;
	public static Status status = Status.IDLE;
	public static ServerConsoleScreen consoleScreen;

	public enum Status {
		IDLE,
		LOADING,
		STOPPING,
		CONNECTING,
		JOINED,
		LEAVING
	}

	@Override
	public void onInitializeClient() {
		loadPresets();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			if (serverProcess != null) {
				serverProcess.destroy();
			}
		}));
		ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
			if (status == Status.CONNECTING) {
				status = Status.JOINED;
			}
		});
		consoleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.slo.serverConsole", InputConstants.KEY_GRAVE, KeyMapping.CATEGORY_INTERFACE));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (consoleScreen != null && consoleKey.isDown()) {
				consoleKey.setDown(false);
				Minecraft.getInstance().execute(() -> Minecraft.getInstance().setScreen(consoleScreen));
			}
		});
		Runtime.getRuntime().addShutdownHook(new Thread(() -> serverProcess.destroy()));
	}

	public static Path presetsDirectory() {
		return FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("presets");
	}

	public static void loadPresets() {
		var presetsDirectory = presetsDirectory();
		if (!presetsDirectory.toFile().exists()) {
			return;
		}
		try (var presets = Files.list(presetsDirectory)) {
			for (var preset : presets.toList()) {
				var levelDirectory = ExtendedLevelDirectory.create(preset, false, false);
				if (levelDirectory.slo$isServer()) {
					worldPresets.put(levelDirectory.slo$directory().directoryName(), levelDirectory);
				}
				else {
					LOGGER.warn("Not a server world preset: {}", preset);
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e); // TODO
		}
		if (!worldPresets.isEmpty()) {
			LOGGER.info("Loaded {} server world preset(s): {}", worldPresets.size(), String.join(", ", worldPresets.keySet()));
		}
	}

	public static void updateCreationState(ExtendedLevelDirectory directory, WorldCreationUiState creationState) {
		((ExtendedWorldCreationUiState) creationState).slo$setPresetDirectory(directory);
		var properties = directory.slo$serverProperties();
		if (properties.containsKey("difficulty")) {
			var difficulty = Difficulty.byName(properties.getProperty("difficulty"));
			if (difficulty != null) {
				creationState.setDifficulty(difficulty);
			}
		}
		if (properties.containsKey("level-seed")) {
			creationState.setSeed(properties.getProperty("level-seed"));
		}
		if (properties.containsKey("level-type")) {
			var levelType = ResourceLocation.tryParse(properties.getProperty("level-type"));
			if (levelType != null) {
				for (var entry : creationState.getAltPresetList()) {
					if (entry.preset() != null && entry.preset().unwrapKey().map(key -> key.location().equals(levelType)).orElse(false)) {
						creationState.setWorldType(entry);
						break;
					}
				}
			}
		}
		if (properties.containsKey("hardcore") && properties.getProperty("hardcore").equals("true")) {
			creationState.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
		}
		else if (properties.containsKey("gamemode")) {
			var gameType = GameType.byName(properties.getProperty("gamemode"));
			var gameMode = switch (gameType) {
				case SURVIVAL, ADVENTURE -> WorldCreationUiState.SelectedGameMode.SURVIVAL;
				case CREATIVE, SPECTATOR -> WorldCreationUiState.SelectedGameMode.CREATIVE;
			};
			creationState.setGameMode(gameMode);
		}
		if (properties.containsKey("generate-structures")) {
			creationState.setGenerateStructures(properties.getProperty("generate-structures").equals("true"));
		}
	}

	public static void load(Minecraft minecraft, Screen screen, ExtendedLevelDirectory directory) throws IOException {
		if (directory.slo$jarCandidates() != null) {
			minecraft.setScreen(new SelectJarCandidateScreen(screen, directory));
		}
		else {
			LoadServerLevelScreen.load(minecraft, screen, directory, null);
		}
	}

	public static void stop(Minecraft minecraft, Status status) {
		Slo.status = status;
		minecraft.setScreen(new GenericMessageScreen(GUI_STOP_SERVER));
		consoleScreen = null;
		serverProcess.destroy();
	}

	public static void writeProperties(WorldCreationUiState creationState) throws IOException {
		var properties = levelDirectory.slo$serverProperties();
		properties.setProperty("difficulty", creationState.getDifficulty().getKey());
		properties.setProperty("level-seed", creationState.getSeed());
		if (creationState.getWorldType().preset() != null) {
			creationState.getWorldType().preset().unwrapKey().ifPresent(key -> properties.setProperty("level-type", key.location().toString()));
		}
		if (!properties.containsKey("gamemode") || !properties.getProperty("gamemode").equals("adventure") || creationState.getGameMode() != WorldCreationUiState.SelectedGameMode.SURVIVAL) {
			properties.setProperty("gamemode", creationState.getGameMode().gameType.getName());
		}
		properties.setProperty("generate-structures", creationState.isGenerateStructures() ? "true" : "false");
		properties.setProperty("hardcore", creationState.isHardcore() ? "true" : "false");
		levelDirectory.slo$writeServerProperties();
	}

	public static void sendStartupCommands(WorldCreationUiState creationUiState) {
		var stdin = Slo.serverProcess.getOutputStream();
		var writer = new BufferedWriter(new OutputStreamWriter(stdin));
		var playerName = Minecraft.getInstance().getGameProfile().getName();
		try {
			if (creationUiState.isAllowCommands()) {
				writer.write("op " + playerName + "\n");
			}
			creationUiState.getGameRules().visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
				@Override
				public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
					try {
						writer.write("gamerule " + key.getId() + " " + creationUiState.getGameRules().getRule(key) + "\n");
					}
					catch (IOException e) {
						Slo.LOGGER.error("Failed to send startup command", e);
					}
				}
			});
			writer.flush();
		}
		catch (IOException e) {
			Slo.LOGGER.error("Failed to send startup commands", e);
		}
	}

	public static void connect(Minecraft minecraft, Screen parent) {
		status = Status.CONNECTING;
		var serverData = new ServerData(levelDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
		ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(serverData.ip), serverData, false, null);
	}

	public static void onExit(Minecraft minecraft, Screen parent) {
		try (var stream = Files.walk(levelDirectory.slo$directory().path())) {
			stream.filter(path -> path.getFileName().endsWith("session.lock"))
				.forEach(path -> path.toFile().delete());
		}
		catch (IOException e) {
			LOGGER.error("Failed to walk level directory", e);
		}
		if (!minecraft.isRunning()) {
			return;
		}
		minecraft.execute(() -> {
			if (status == Status.STOPPING) {
				minecraft.forceSetScreen(parent);
			}
			else if (status == Status.LOADING) {
				minecraft.forceSetScreen(new DisconnectedScreen(parent, GUI_START_SERVER_FAIL, GUI_START_SERVER_FAIL_INFO, GUI_TO_WORLD));
			}
			else {
				minecraft.forceSetScreen(new TitleScreen());
			}
			serverProcess = null;
			status = Status.IDLE;
		});
		levelDirectory = null;
	}
}
