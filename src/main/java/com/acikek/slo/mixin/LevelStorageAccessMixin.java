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

	@Shadow
	@Final
	LevelStorageSource.LevelDirectory levelDirectory;

	@Inject(method = "renameLevel", at = @At("HEAD"), cancellable = true)
	private void slo$renameLevel(String string, CallbackInfo ci) throws IOException {
		var extended = (ExtendedLevelDirectory) (Object) levelDirectory;
		if (extended != null && extended.slo$isServer()) {
			ci.cancel();
			extended.slo$setLevelName(string);
			extended.slo$writeSloProperties();
		}
	}
}
