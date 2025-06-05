package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ServerLevelSummary;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

@Mixin(LevelStorageSource.class)
public class LevelStorageSourceMixin {

	@Inject(method = "method_29015(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;ZLjava/nio/file/Path;Lcom/mojang/datafixers/DataFixer;)Lnet/minecraft/world/level/storage/LevelSummary;", at = @At(value = "RETURN", ordinal = 1), cancellable = true)
	private void slo$makeServerLevelSummary(LevelStorageSource.LevelDirectory levelDirectory, boolean locked, Path path, DataFixer dataFixer, CallbackInfoReturnable<LevelSummary> cir, @Local LevelVersion levelVersion, @Local(ordinal = 1) boolean requiresManualConversion, @Local(ordinal = 1) Path icon, @Local LevelSettings levelSettings, @Local(ordinal = 2) boolean experimental) {
		var extended = (ExtendedLevelDirectory) (Object) levelDirectory;
		if (extended != null && extended.slo$isServer()) {
			cir.setReturnValue(new ServerLevelSummary(levelSettings, levelVersion, levelDirectory.directoryName(), requiresManualConversion, locked, experimental, icon, levelDirectory));
		}
	}
}
