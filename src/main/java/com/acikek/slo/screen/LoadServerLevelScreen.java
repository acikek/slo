package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.FileUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;

public class LoadServerLevelScreen extends Screen {

    //public static final Component START_SERVER_FAIL =

    public Screen parent;
    public ServerLevelSummary summary;
    public Process process;

    public Component status = Component.literal("Starting server...");
    public boolean completed;
    public boolean cancelled;

    public LoadServerLevelScreen(Screen parent, ServerLevelSummary summary, Process process) {
        super(Component.literal("Loading Server Level"));
        this.parent = parent;
        this.summary = summary;
        this.process = process;
    }

    public static void load(Minecraft minecraft, Screen parent, ServerLevelSummary summary) throws IOException {
        var processBuilder = new ProcessBuilder(summary.extendedDirectory.slo$processArgs());
        processBuilder.directory(summary.directory.path().toFile());
        var process = processBuilder.start();
        var loadScreen = new LoadServerLevelScreen(parent, summary, process);
        minecraft.setScreen(loadScreen);
        loadScreen.startProcessInputThread();
        process.onExit().thenAccept(exited -> loadScreen.onProcessExit(minecraft, exited));
    }

    public void startProcessInputThread() {
        new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                handleProcessInput(reader);
            } catch (IOException e) {
                if (!cancelled) {
                    Slo.LOGGER.error("Failed to read server input", e);
                }
            }
        }).start();
    }

    public void handleProcessInput(BufferedReader reader) throws IOException {
        String line;
        boolean preparing = false;
        var logger = LoggerFactory.getLogger(Slo.MOD_ID + "/" + summary.extendedDirectory.slo$levelName());
        while ((line = reader.readLine()) != null) {
            logger.info(line);
            if (line.contains("Starting minecraft server version")) {
                status = Component.literal("Initializing server...");
            }
            else if (!preparing && line.contains("Preparing level")) {
                status = Component.literal("Loading worlds...");
                preparing = true;
            }
            else if (line.contains("For help, type \"help\"")) {
                var serverData = new ServerData(summary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
                minecraft.execute(() -> {
                    ConnectScreen.startConnecting(parent, minecraft, ServerAddress.parseString("localhost"), serverData, false, null);
                    Slo.serverProcess = process;
                    completed = true;
                });
            }
        }
    }

    public void onProcessExit(Minecraft minecraft, Process process) {
        if (cancelled) {
            try (var stream = Files.walk(summary.directory.path())) {
                stream.filter(path -> path.getFileName().endsWith("session.lock"))
                        .forEach(path -> path.toFile().delete());
            } catch (IOException e) {
                Slo.LOGGER.error("Failed to walk level directory", e);
            }
        }
        minecraft.execute(() -> {
            if (cancelled) {
                minecraft.forceSetScreen(parent);
            }
            else if (!completed) {
                minecraft.forceSetScreen(new DisconnectedScreen(parent, Component.translatable("gui.slo.startServerFail"), Component.translatable("gui.slo.startServerFail.info", process.exitValue()), Component.translatable("gui.toWorld")));
            }
            else {
                minecraft.forceSetScreen(new TitleScreen());
            }
        });
        Slo.serverProcess = null;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            minecraft.setScreen(new GenericMessageScreen(Component.literal("Stopping server")));
            cancelled = true;
            process.destroy();
        }).bounds(width / 2 - 100, height / 4 + 120 + 12, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(font, status, width / 2, height / 2 - 50, 0xFFFFFF);
    }
}
