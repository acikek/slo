package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.acikek.slo.screen.ServerProcessScreen;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin {

    @ModifyExpressionValue(method = "createPauseMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean slo$useSingleplayerButton(boolean original) {
        return Slo.status == Slo.Status.IDLE || Slo.status == Slo.Status.JOINED;
    }

    @Inject(method = "onDisconnect", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;disconnect()V"), cancellable = true)
    private void slo$disconnectToTitle(CallbackInfo ci) {
        if (Slo.status == Slo.Status.JOINED) {
            ci.cancel();
            Slo.status = Slo.Status.LEAVING;
            Minecraft.getInstance().disconnect(new ServerProcessScreen.ShutDown());
        }
    }
}
