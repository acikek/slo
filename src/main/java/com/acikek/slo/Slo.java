package com.acikek.slo;

import com.acikek.slo.screen.LoadServerLevelScreen;
import com.acikek.slo.screen.SelectJarCandidateScreen;
import com.acikek.slo.util.ExtendedLevelDirectory;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Slo implements ModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static final Component GUI_START_SERVER_FAIL = Component.translatable("gui.slo.startServerFail");
	public static final Component GUI_START_SERVER_FAIL_INFO = Component.translatable("gui.slo.startServerFail.info");
	public static final Component GUI_STOP_SERVER = Component.translatable("gui.slo.status.stopServer");
	public static final Component GUI_RETRY = Component.translatable("gui.slo.retry");
	public static final Component GUI_TO_WORLD = Component.translatable("gui.toWorld");

	public static Map<String, ExtendedLevelDirectory> worldPresets = new HashMap<>();

	public static boolean directoryInitUpdate = true;
	public static boolean directoryInitAutodetect = true;

	public static Process serverProcess;
	public static ExtendedLevelDirectory levelDirectory;
	public static Status status = Status.IDLE;

	public enum Status {
		IDLE,
		LOADING,
		STOPPING,
		CONNECTING,
		JOINED,
		LEAVING
	}

	@Override
	public void onInitialize() {
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
	}

	public static void loadPresets() {
		var presetsFolder = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("presets");
		if (!presetsFolder.toFile().exists()) {
			return;
		}
		try (var presets = Files.list(presetsFolder)) {
			for (var preset : presets.toList()) {
				var levelDirectory = ExtendedLevelDirectory.create(preset, false, false);
				if (levelDirectory.slo$isServer()) {
					var presetName = Util.sanitizeName(preset.getFileName().toString(), ResourceLocation::validPathChar);
					worldPresets.put(presetName, levelDirectory);
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

	public static void load(Minecraft minecraft, Screen screen, ExtendedLevelDirectory directory) throws IOException {
		if (directory.slo$jarCandidates() != null) {
			minecraft.setScreen(new SelectJarCandidateScreen(screen, directory));
		}
		else {
			LoadServerLevelScreen.load(minecraft, screen, directory, null);
		}
	}

	public static void stop(Minecraft minecraft) {
		Slo.status = Status.STOPPING;
		minecraft.setScreen(new GenericMessageScreen(GUI_STOP_SERVER));
		serverProcess.destroy();
	}

	public static void writeProperties(WorldCreationUiState creationState) throws IOException {
		var properties = levelDirectory.slo$serverProperties();
		properties.setProperty("difficulty", creationState.getDifficulty().getKey());
		properties.setProperty("level-seed", creationState.getSeed());
		// TODO: level-type
		properties.setProperty("gamemode", creationState.getGameMode().gameType.getName());
		properties.setProperty("generate-structures", creationState.isGenerateStructures() ? "true" : "false");
		properties.setProperty("hardcore", creationState.isHardcore() ? "true" : "false");
		levelDirectory.slo$writeServerProperties();
	}

	public static void sendStartupCommands(WorldCreationUiState creationUiState) throws IOException {
		var stdin = Slo.serverProcess.getOutputStream();
		var writer = new BufferedWriter(new OutputStreamWriter(stdin));
		var playerName = Minecraft.getInstance().getGameProfile().getName();
		if (creationUiState.isAllowCommands()) {
			writer.write("op " + playerName + "\n");
		}
		creationUiState.getGameRules().visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
			@Override
			public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                try {
                    writer.write("gamerule " + key.getId() + " " + creationUiState.getGameRules().getRule(key) + "\n");
                } catch (IOException e) { // TODO bruh
                    throw new RuntimeException(e);
                }
            }
		});
		writer.flush();
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
		} catch (IOException e) {
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
