package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.acikek.slo.screen.LoadServerLevelScreen;
import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.apache.commons.io.FileUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Optional;

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
        Optional.ofNullable(uiState.getWorldType().preset())
                .flatMap(Holder::unwrapKey)
                .map(ResourceKey::location)
                .filter(location -> location.getNamespace().equals(Slo.MOD_ID))
                .ifPresent(location -> {
                    ci.cancel();
                    var presetDirectory = Slo.worldPresets.get(location.getPath());
                    var levelSource = Minecraft.getInstance().getLevelSource();
                    // TODO: check if this is indeed a server level before blindly jumping in
                    // TODO: handle properly
                    try {
                        var targetDirectory = levelSource.getLevelPath(uiState.getTargetFolder());
                        FileUtils.copyDirectory(presetDirectory.slo$directory().path().toFile(), targetDirectory.toFile());
                        var newLevelDirectory = ExtendedLevelDirectory.create(targetDirectory, true, false);
                        LoadServerLevelScreen.load(Minecraft.getInstance(), (Screen) (Object) this, newLevelDirectory, uiState);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
