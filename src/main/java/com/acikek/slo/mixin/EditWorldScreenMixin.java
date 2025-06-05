package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.Util;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(EditWorldScreen.class)
public class EditWorldScreenMixin {

	@Shadow
	@Final
	private LevelStorageSource.LevelStorageAccess levelAccess;

	@ModifyArg(method = "init", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 2))
	private Button.OnPress slo$openServerDirectory(Button.OnPress onPress) {
		return levelAccess.getSummary() instanceof ServerLevelSummary summary
			? button -> Util.getPlatform().openFile(summary.extendedDirectory.slo$directory().path().toFile())
			: onPress;
	}
}
