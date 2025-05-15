package com.acikek.slo.mixin;

import com.acikek.slo.ExtendedLevelDirectory;
import com.acikek.slo.ServerLevelSummary;
import com.mojang.serialization.Dynamic;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.nio.file.Path;

@Mixin(LevelStorageSource.class)
public class LevelStorageSourceMixin {

    @Inject(method = "makeLevelSummary", at = @At(value = "RETURN"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void a(Dynamic<?> dynamic, LevelStorageSource.LevelDirectory levelDirectory, boolean locked, CallbackInfoReturnable<LevelSummary> cir, LevelVersion levelVersion, int i, boolean requiresManualConversion, Path icon, WorldDataConfiguration worldDataConfiguration, LevelSettings levelSettings, FeatureFlagSet featureFlagSet, boolean experimental) {
        var extended = (ExtendedLevelDirectory) (Object) levelDirectory;
        if (extended != null && extended.slo$isServer()) {
            cir.setReturnValue(new ServerLevelSummary(levelSettings, levelVersion, levelDirectory.directoryName(), requiresManualConversion, locked, experimental, icon, levelDirectory));
        }
    }
}
