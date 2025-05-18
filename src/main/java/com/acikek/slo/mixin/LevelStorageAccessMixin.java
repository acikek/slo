package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.function.Consumer;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {

    @Shadow @Final LevelStorageSource.LevelDirectory levelDirectory;

    @Shadow protected abstract void modifyLevelDataWithoutDatafix(Consumer<CompoundTag> consumer) throws IOException;

    @Inject(method = "renameLevel", at = @At("HEAD"), cancellable = true)
    private void slo$renameLevel(String string, CallbackInfo ci) throws IOException {
        if (slo$renameLevel(string, false)) {
            ci.cancel();
        }
    }

    @Inject(method = "renameAndDropPlayer", at = @At("HEAD"), cancellable = true)
    private void slo$renameAndDropPlayer(String string, CallbackInfo ci) throws IOException {
        if (slo$renameLevel(string, true)) {
            ci.cancel();
        }
    }

    @Unique
    private boolean slo$renameLevel(String levelName, boolean dropPlayer) throws IOException {
        var extended = (ExtendedLevelDirectory) (Object) levelDirectory;
        if (extended == null || !extended.slo$isServer()) {
            return false;
        }
        extended.slo$setLevelName(levelName);
        extended.slo$writeProperties();
        if (dropPlayer) {
            modifyLevelDataWithoutDatafix((compoundTag) -> {
                compoundTag.remove("Player");
            });
        }
        return true;
    }
}
