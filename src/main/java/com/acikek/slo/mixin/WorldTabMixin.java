package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedWorldCreationUiState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.WorldTab.class)
public class WorldTabMixin {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 1))
    private void slo$captureCycleButton(CreateWorldScreen createWorldScreen, CallbackInfo ci, @Local CycleButton<WorldCreationUiState.WorldTypeEntry> cycleButton) {
        var extendedUiState = ((ExtendedWorldCreationUiState) createWorldScreen.getUiState());
        extendedUiState.slo$setWorldTypeButton(cycleButton);
        extendedUiState.slo$updateWorldTypeButton();
    }
}
