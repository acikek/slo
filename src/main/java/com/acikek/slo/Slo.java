package com.acikek.slo;

import com.acikek.slo.util.ServerLevelSummary;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
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

	public static final Component GUI_START_SERVER_FAIL = Component.translatable("gui.slo.startServerFail");
	public static final Component GUI_START_SERVER_FAIL_INFO = Component.translatable("gui.slo.startServerFail.info");
	public static final Component GUI_STOP_SERVER = Component.translatable("gui.slo.status.stopServer");
	public static final Component GUI_RETRY = Component.translatable("gui.slo.retry");
	public static final Component GUI_TO_WORLD = Component.translatable("gui.toWorld");

	public static Process serverProcess;
	public static ServerLevelSummary levelSummary;
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
		// SymlinkLevelSummary?
		Runtime.getRuntime().addShutdownHook(new Thread(() -> serverProcess.destroy()));
		ClientPlayConnectionEvents.JOIN.register((clientPacketListener, packetSender, minecraft) -> {
			if (status == Status.CONNECTING) {
				status = Status.JOINED;
			}
		});
	}

	public static void stop(Minecraft minecraft) {
		Slo.status = Status.STOPPING;
		minecraft.setScreen(new GenericMessageScreen(GUI_STOP_SERVER));
		serverProcess.destroy();
	}

	public static void connect(Minecraft minecraft, Screen parent) {
		status = Status.CONNECTING;
		var serverData = new ServerData(levelSummary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
		ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString(serverData.ip), serverData, false, null);
	}

	public static void onExit(Minecraft minecraft, Screen parent) {
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
		levelSummary = null;
	}
}
