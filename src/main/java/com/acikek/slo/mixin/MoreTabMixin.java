package com.acikek.slo.mixin;

import com.acikek.slo.screen.SelectServerTypeScreen;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreateWorldScreen.MoreTab.class)
public class MoreTabMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void slo$addServerButton(CreateWorldScreen createWorldScreen, CallbackInfo ci, @Local GridLayout.RowHelper rowHelper) {
        rowHelper.addChild(Button.builder(
                Component.literal("Server Type"),
                button -> Minecraft.getInstance().setScreen(new SelectServerTypeScreen(createWorldScreen, (ExtendedWorldCreationUiState) createWorldScreen.getUiState())))
                .width(210).build());
    }
}
