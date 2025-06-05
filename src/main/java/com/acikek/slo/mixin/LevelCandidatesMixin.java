package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ExtendedLevelDirectory;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LevelStorageSource.LevelCandidates.class)
public class LevelCandidatesMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	private void slo$logServerLevelCandidates(List<LevelStorageSource.LevelDirectory> list, CallbackInfo ci) {
		var found = list.stream().filter(directory -> ((ExtendedLevelDirectory) (Object) directory).slo$isServer()).toList();
		if (!found.isEmpty()) {
			var foundList = String.join(", ", found.stream().map(LevelStorageSource.LevelDirectory::directoryName).toList());
			Slo.LOGGER.info("Found {} server level candidate(s): {}", found.size(), foundList);
		}
	}
}
