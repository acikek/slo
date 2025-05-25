package com.acikek.slo.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.WorldTab.class)
public class WorldTabMixin {

    @Shadow @Final private EditBox seedEdit;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void slo$addListeners(CreateWorldScreen createWorldScreen, CallbackInfo ci, @Local CycleButton<WorldCreationUiState.WorldTypeEntry> cycleButton) {
        seedEdit.setResponder(seed -> {
            if (!createWorldScreen.getUiState().getSeed().equals(seed)) {
                createWorldScreen.getUiState().setSeed(seed);
            }
        });
        createWorldScreen.getUiState().addListener(uiState -> {
            if (!seedEdit.getValue().equals(uiState.getSeed())) {
                seedEdit.setValue(uiState.getSeed());
            }
        });
    }
}
