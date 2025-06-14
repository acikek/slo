package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
	private void slo$stopServerProcess(CallbackInfo ci, @Local(argsOnly = true) Screen screen) {
		if (Slo.status != Slo.Status.LEAVING) {
			return;
		}
		var stdin = Slo.serverProcess.getOutputStream();
		var writer = new BufferedWriter(new OutputStreamWriter(stdin));
		try {
			writer.write("stop");
			writer.flush();
			writer.close();
		}
		catch (IOException e) {
			Slo.LOGGER.error("Failed to shut down server", e);
		}
	}
}
