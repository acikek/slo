package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LoadServerLevelScreen extends Screen {

    //public static final Component START_SERVER_FAIL =

    public Screen parent;

    public Component status = Component.literal("Starting server...");

    public LoadServerLevelScreen(Screen parent) {
        super(GameNarrator.NO_TITLE);
        this.parent = parent;
    }

    public static void load(Minecraft minecraft, Screen parent, ServerLevelSummary summary) throws IOException {
        Slo.levelSummary = summary;
        var processBuilder = new ProcessBuilder(Slo.levelSummary.extendedDirectory.slo$processArgs());
        processBuilder.directory(Slo.levelSummary.directory.path().toFile());
        Slo.serverProcess = processBuilder.start();
        var loadScreen = new LoadServerLevelScreen(parent);
        minecraft.setScreen(loadScreen);
        loadScreen.startProcessInputThread();
        Slo.serverProcess.onExit().thenAccept(exited -> Slo.onServerProcessExit(minecraft, parent));
    }

    public void startProcessInputThread() {
        new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(Slo.serverProcess.getInputStream()))) {
                handleProcessInput(reader);
            } catch (IOException e) {
                if (!Slo.startCancelled) {
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
                status = Component.literal("Initializing server...");
            }
            else if (!preparing && line.contains("Preparing level")) {
                status = Component.literal("Loading worlds...");
                preparing = true;
            }
            else if (line.contains("For help, type \"help\"")) {
                minecraft.execute(() -> Slo.connect(minecraft, parent));
            }
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> Slo.cancelServerStart(minecraft))
                .bounds(width / 2 - 100, height / 4 + 120 + 12, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(font, status, width / 2, height / 2 - 50, 0xFFFFFF);
    }
}
