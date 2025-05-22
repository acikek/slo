package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow private long lastScreenshotAttempt;

    @Shadow private boolean hasWorldScreenshot;

    @Shadow protected abstract void takeAutoScreenshot(Path path);

    @Inject(method = "tryTakeScreenshotIfNeeded", at = @At("HEAD"))
    private void slo$takeAutoScreenshot(CallbackInfo ci) {
        if (Slo.status != Slo.Status.JOINED || Slo.levelDirectory == null || !Slo.levelDirectory.slo$autoScreenshot()) {
            return;
        }
        long l = Util.getMillis();
        if (l - lastScreenshotAttempt < 1000L) {
            return;
        }
        var iconPath = Slo.levelDirectory.slo$directory().iconFile();
        if (Files.isRegularFile(iconPath)) {
            hasWorldScreenshot = true;
        }
        else {
            takeAutoScreenshot(iconPath);
        }
    }
}
