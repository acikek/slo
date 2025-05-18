package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LoadServerLevelScreen extends ServerProcessScreen {

    //public static final Component START_SERVER_FAIL =

    public Screen parent;

    public LoadServerLevelScreen(Screen parent) {
        super(Component.literal("Starting server..."), Component.literal("Cancel"));
        this.parent = parent;
    }

    public static void load(Minecraft minecraft, Screen parent, ServerLevelSummary summary) throws IOException {
        Slo.levelSummary = summary;
        var processBuilder = new ProcessBuilder(Slo.levelSummary.extendedDirectory.slo$processArgs());
        processBuilder.directory(Slo.levelSummary.directory.path().toFile());
        Slo.serverProcess = processBuilder.start();
        Slo.status = Slo.Status.LOADING;
        var loadScreen = new LoadServerLevelScreen(parent);
        minecraft.setScreen(loadScreen);
        loadScreen.startProcessInputThread();
        Slo.serverProcess.onExit().thenAccept(exited -> Slo.onExit(minecraft, parent));
    }

    public void startProcessInputThread() {
        new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(Slo.serverProcess.getInputStream()))) {
                handleProcessInput(reader);
            } catch (IOException e) {
                if (Slo.status != Slo.Status.STOPPING) {
                    Slo.LOGGER.error("Failed to read server input", e);
                }
            }
        }).start();
    }

    public void handleProcessInput(BufferedReader reader) throws IOException {
        String line;
        boolean preparing = false;
        var logger = LoggerFactory.getLogger(Slo.MOD_ID + "/" + Slo.levelSummary.extendedDirectory.slo$levelName());
        while ((line = reader.readLine()) != null) {
            logger.info(line);
            if (line.contains("Starting minecraft server version")) {
                setStatus(Component.literal("Initializing server..."));
            }
            else if (!preparing && line.contains("Preparing level")) {
                setStatus(Component.literal("Loading worlds..."));
                preparing = true;
            }
            else if (line.contains("For help, type \"help\"")) {
                Slo.connect(minecraft, parent);
            }
        }
    }

    @Override
    public void exit() {
        Slo.stop(minecraft);
    }
}
