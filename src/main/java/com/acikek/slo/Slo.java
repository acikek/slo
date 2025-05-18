package com.acikek.slo;

import com.acikek.slo.util.ServerLevelSummary;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Slo implements ModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static Process serverProcess;
	public static ServerLevelSummary levelSummary;
	public static boolean startCancelled;
	public static boolean startComplete;

	@Override
	public void onInitialize() {
		// SymlinkLevelSummary?
		Runtime.getRuntime().addShutdownHook(new Thread(() -> serverProcess.destroy()));
	}

	public static void cancelServerStart(Minecraft minecraft) {
		minecraft.setScreen(new GenericMessageScreen(Component.literal("Stopping server")));
		serverProcess.destroy();
		startCancelled = true;
	}

	public static void connect(Minecraft minecraft, Screen parent) {
		startComplete = true;
		var serverData = new ServerData(levelSummary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
		ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(serverData.ip), serverData, false, null);
	}

	public static void onServerProcessExit(Minecraft minecraft, Screen parent) {
		try (var stream = Files.walk(levelSummary.directory.path())) {
			stream.filter(path -> path.getFileName().endsWith("session.lock"))
					.forEach(path -> path.toFile().delete());
		} catch (IOException e) {
			LOGGER.error("Failed to walk level directory", e);
		}
		if (!minecraft.isRunning()) {
			return;
		}
		minecraft.execute(() -> {
			if (startCancelled) {
				minecraft.forceSetScreen(parent);
			}
			else if (!startComplete) {
				minecraft.forceSetScreen(new DisconnectedScreen(parent, Component.translatable("gui.slo.startServerFail"), Component.translatable("gui.slo.startServerFail.info", serverProcess.exitValue()), Component.translatable("gui.toWorld")));
			}
			else {
				minecraft.forceSetScreen(new TitleScreen());
			}
			serverProcess = null;
			startComplete = false;
			startCancelled = false;
		});
		levelSummary = null;
	}
}
