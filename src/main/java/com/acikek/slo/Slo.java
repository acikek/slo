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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;

public class Slo implements ModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static Process serverProcess;

	@Override
	public void onInitialize() {
		// SymlinkLevelSummary?
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			serverProcess.destroy();
		}));
	}

	public static void connect(Minecraft minecraft, Screen parent, ServerLevelSummary serverLevelSummary) throws IOException, ExecutionException, InterruptedException {
		var builder = new ProcessBuilder(Slo.JAVA_PATH, "-jar", serverLevelSummary.extendedDirectory.slo$jarPath());
		builder.directory(serverLevelSummary.directory.path().toFile());
		var process = builder.start();
		minecraft.execute(() -> {
			minecraft.setScreen(new GenericMessageScreen(Component.literal("Starting server...")));
		});
		new Thread(() -> {
			try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				boolean preparing = false;
				var logger = LoggerFactory.getLogger(MOD_ID + "/" + serverLevelSummary.extendedDirectory.slo$levelName());
				while ((line = reader.readLine()) != null) {
					logger.info(line);
					if (line.contains("Starting minecraft server version")) {
						minecraft.execute(() -> minecraft.setScreen(new GenericMessageScreen(Component.literal("Initializing server..."))));
					}
					else if (!preparing && line.contains("Preparing level")) {
						minecraft.execute(() -> minecraft.setScreen(new GenericMessageScreen(Component.literal("Loading worlds..."))));
						preparing = true;
					}
					else if (line.contains("For help, type \"help\"")) {
						minecraft.execute(() -> {
							var serverData = new ServerData(serverLevelSummary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
							ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString("localhost"), serverData, false, null);
							Slo.serverProcess = process;
						});
					}
				}
			} catch (IOException e) {
				Slo.LOGGER.error("Failed to read server input", e);
			}
        }).start();
		process.onExit().thenAccept(exited ->
			minecraft.execute(() -> {
				if (Slo.serverProcess == null) {
					var disconnectedScreen = new DisconnectedScreen(parent, Component.literal("Failed to start the server"), Component.literal("Exit code: " + exited.exitValue() + ". See logs for more details."), Component.translatable("gui.toWorld"));
					minecraft.forceSetScreen(disconnectedScreen);
				}
				else {
					minecraft.forceSetScreen(new TitleScreen());
				}
				Slo.serverProcess = null;
			})
		);
	}
}
