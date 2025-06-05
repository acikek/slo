package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {

	@Inject(method = "onDisconnect", at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;callbackScreen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0), cancellable = true)
	private void slo$stopServerProcess(CallbackInfo ci) {
		if (Slo.status != Slo.Status.LEAVING) {
			return;
		}
		ci.cancel();
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
