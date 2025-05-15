package com.acikek.slo.mixin;

import com.acikek.slo.ServerLevelSummary;
import com.acikek.slo.Slo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldListEntryMixin {

    @Shadow @Final LevelSummary summary;

    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void a(CallbackInfo ci) throws IOException, ExecutionException, InterruptedException {
        if (!summary.primaryActionActive() || !(summary instanceof ServerLevelSummary serverLevelSummary)) {
            return;
        }
        ci.cancel();
        CompletableFuture<Void> loaded = new CompletableFuture<>();
        var builder = new ProcessBuilder(Slo.JAVA_PATH, "-jar", serverLevelSummary.extendedDirectory.slo$jarPath());
        builder.directory(serverLevelSummary.directory.path().toFile());
        Slo.serverProcess = builder.start();
        var readThread = new Thread(() -> {
            try (var reader = new BufferedReader(new InputStreamReader(Slo.serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Slo.LOGGER.info(line);
                    if (line.contains("For help, type \"help\"")) {
                        loaded.complete(null);
                    }
                }
            } catch (IOException e) {
                Slo.LOGGER.error("bruh", e);
            }
        });
        readThread.start();
        loaded.get();
        var serverData = new ServerData(serverLevelSummary.extendedDirectory.slo$levelName(), "localhost", ServerData.Type.OTHER);
        ConnectScreen.startConnecting(Minecraft.getInstance().screen, Minecraft.getInstance(), ServerAddress.parseString("localhost"), serverData, false, null);
    }
}
