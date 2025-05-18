package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public class DisconnectedScreenMixin {

    @Shadow @Final private LinearLayout layout;

    @Shadow @Final private Screen parent;

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V"))
    private void slo$addRetryButton(CallbackInfo ci) {
        if (Slo.startComplete) {
            layout.addChild(Button.builder(Component.literal("Retry"), button -> Slo.connect(Minecraft.getInstance(), parent)).build());
        }
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 2))
    private <T extends LayoutElement> T slo$modifyBackButton(T layoutElement) {
        return Slo.startComplete
                ? (T) Button.builder(Component.translatable("gui.toWorld"), button -> Slo.cancelServerStart(Minecraft.getInstance())).build()
                : layoutElement;
    }
}
