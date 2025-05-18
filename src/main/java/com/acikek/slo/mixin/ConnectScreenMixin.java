package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

    @WrapWithCondition(method = "startConnecting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;disconnect()V"))
    private static boolean slo$stopDisconnect(Minecraft instance) {
        return !Slo.startComplete;
    }
}
