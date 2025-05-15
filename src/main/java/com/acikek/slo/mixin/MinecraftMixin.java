package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void slo$stopServerProcess(CallbackInfo ci) throws IOException {
        if (Slo.serverProcess != null) {
            var stdin = Slo.serverProcess.getOutputStream();
            var writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write("stop");
            writer.flush();
            writer.close();
        }
    }
}
