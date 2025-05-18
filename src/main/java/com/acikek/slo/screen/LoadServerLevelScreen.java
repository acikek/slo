package com.acikek.slo.screen;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class LoadServerLevelScreen extends Screen {

    //public static final Component START_SERVER_FAIL =

    public Screen parent;
    public Component status;

    public LoadServerLevelScreen(Screen parent) {
        super(Component.literal("Loading Server Level"));
        this.parent = parent;
        status = Component.literal("Starting server...");
    }

    public static void load(Minecraft minecraft, Screen parent, ServerLevelSummary summary) throws IOException {
        var processBuilder = new ProcessBuilder(summary.extendedDirectory.slo$processArgs());
        processBuilder.directory(summary.directory.path().toFile());
        var process = processBuilder.start();
        var loadScreen = new LoadServerLevelScreen(parent);
        minecraft.setScreen(loadScreen);
        new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                loadScreen.handleProcessInput(minecraft, summary, process, reader);
            } catch (IOException e) {
                Slo.LOGGER.error("Failed to read server input", e);
            }
        }).start();
        process.onExit().thenAccept(exited -> loadScreen.onProcessExit(minecraft, exited));
    }

    public void handleProcessInput(Minecraft minecraft, ServerLevelSummary summary, Process process, BufferedReader reader) throws IOException {
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
                });
            }
        }
    }

    public void onProcessExit(Minecraft minecraft, Process process) {
        minecraft.execute(() -> {
            // TODO: ternary
            if (Slo.serverProcess == null) {
                minecraft.forceSetScreen(new DisconnectedScreen(parent, Component.translatable("gui.slo.startServerFail"), Component.translatable("gui.slo.startServerFail.info", process.exitValue()), Component.translatable("gui.toWorld")));
            }
            else {
                minecraft.forceSetScreen(new TitleScreen());
            }
            Slo.serverProcess = null;
        });
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> {
            minecraft.setScreen(parent);
        }).bounds(width / 2 - 100, height / 4 + 120 + 12, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        super.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(font, status, width / 2, height / 2 - 50, 0xFFFFFF);
    }
}
