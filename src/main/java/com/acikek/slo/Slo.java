package com.acikek.slo;

import com.acikek.slo.util.ServerLevelSummary;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Slo implements ModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static final Pattern PREPARING_SPAWN = Pattern.compile("Preparing spawn area: ([0-9])+%");

	public static Process serverProcess;

	@Override
	public void onInitialize() {
		// SymlinkLevelSummary?
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			serverProcess.destroy();
		}));
	}

	/*public enum ConnectPhase {
		INIT,
		LOADING,
		READY,
		ERROR
	}

	public record ConnectInfo(ConnectPhase phase, int progress) {

		public ConnectInfo(ConnectPhase phase) {
			this(phase, 0);
		}
	}*/

	public static void connect(Minecraft minecraft, ServerLevelSummary serverLevelSummary) throws IOException, ExecutionException, InterruptedException {
		//BlockingQueue<ConnectInfo> queue = new LinkedBlockingQueue<>();
		var builder = new ProcessBuilder(Slo.JAVA_PATH, "-jar", serverLevelSummary.extendedDirectory.slo$jarPath());
		builder.directory(serverLevelSummary.directory.path().toFile());
		Slo.serverProcess = builder.start();
		minecraft.execute(() -> {
			minecraft.setScreen(new GenericMessageScreen(Component.literal("Starting server...")));
		});
		new Thread(() -> {
			var progressListener = LoggerChunkProgressListener.create(4);
			try (var reader = new BufferedReader(new InputStreamReader(Slo.serverProcess.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					Slo.LOGGER.info(line);
					if (line.contains("Starting minecraft server version")) {
						//queue.put(new ConnectInfo(ConnectPhase.INIT));
						minecraft.execute(() -> {
							minecraft.setScreen(new GenericMessageScreen(Component.literal("Initializing server...")));
						});
					}
					if (line.contains("Preparing level")) {
						minecraft.execute(() -> {
							minecraft.setScreen(new GenericMessageScreen(Component.literal("Loading worlds...")));
						});
					}
					//var prepMatcher = PREPARING_SPAWN.matcher(line);
					//if (prepMatcher.find())
					/*if (line.contains("Preparing spawn area: ")) {
						//queue.put(new ConnectInfo(ConnectPhase.LOADING));
						minecraft.execute(() -> {
							minecraft.setScreen(new GenericMessageScreen(Component.literal("loading")));
						});
					}*/
					if (line.contains("For help, type \"help\"")) {
						//queue.put(new ConnectInfo(ConnectPhase.READY));
						minecraft.execute(() -> {
							var serverData = new ServerData(serverLevelSummary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
							ConnectScreen.startConnecting(minecraft.screen, minecraft, ServerAddress.parseString("localhost"), serverData, false, null);
						});
					}
				}
			} catch (IOException e) {
				// TODO: probably exit
				Slo.LOGGER.error("Failed to read line", e);
			}/* catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
        }).start();
		Slo.serverProcess.onExit().thenAccept(process -> {
            /*try {
                queue.put(new ConnectInfo(ConnectPhase.ERROR));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }*/
			minecraft.execute(() ->
				minecraft.forceSetScreen(new DisconnectedScreen(minecraft.screen, CommonComponents.CONNECT_FAILED, Component.literal("exit code: " + process.exitValue())))
			);
			Slo.serverProcess = null;
		});
		/*new Thread(() -> {
			while (true) {
				var info = queue.poll(100, TimeUnit.MILLISECONDS);
				if (info == null) {
					continue;
				}
				switch (info.phase()) {
					case INIT -> {
						minecraft.setScreen(new GenericMessageScreen(Component.literal("initializing")));
					}
					case LOADING -> {
						minecraft.setScreen(new GenericMessageScreen(Component.literal("loading")));
					}
					case READY -> {

						return;
					}
				}
			}
		});*/

		//
		//loaded.get();

	}
}
