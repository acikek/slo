package com.acikek.slo.mixin;

import com.acikek.slo.screen.LoadServerLevelScreen;
import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    @Shadow @Final WorldCreationUiState uiState;

    // copy folder
    // modify relevant files
    // make level summary
    // load server level screen
    // send startup commands

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void slo$createServerLevel(CallbackInfo ci) {
        var presetDirectory = ((ExtendedWorldCreationUiState) uiState).slo$presetDirectory();
        if (presetDirectory == null) {
            return;
        }
        ci.cancel();
        var levelSource = Minecraft.getInstance().getLevelSource();
        try {
            var targetDirectory = levelSource.getLevelPath(uiState.getTargetFolder());
            FileUtils.copyDirectory(presetDirectory.slo$directory().path().toFile(), targetDirectory.toFile());
            var newLevelDirectory = ExtendedLevelDirectory.create(targetDirectory, true, false);
            LoadServerLevelScreen.load(Minecraft.getInstance(), (Screen) (Object) this, newLevelDirectory, uiState);
        } catch (IOException e) {
            throw new RuntimeException(e); // TODO
        }
    }
}
