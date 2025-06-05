package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.acikek.slo.screen.LoadServerLevelScreen;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public class ConnectScreenMixin {

	@WrapWithCondition(method = "startConnecting", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
	private static boolean slo$keepLoadScreen(Minecraft instance, Screen screen) {
		return Slo.status == Slo.Status.IDLE;
	}

	@Inject(method = "updateStatus", at = @At("TAIL"))
	private void slo$updateLoadScreenStatus(Component component, CallbackInfo ci) {
		if (Minecraft.getInstance().screen instanceof LoadServerLevelScreen screen) {
			screen.setStatus(component);
		}
	}
}
