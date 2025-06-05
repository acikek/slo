package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.acikek.slo.util.ServerLevelSummary;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.List;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldListEntryMixin {

	@Shadow
	@Final
	LevelSummary summary;

	@Shadow
	@Final
	private Minecraft minecraft;

	@Shadow
	@Final
	private SelectWorldScreen screen;

	@Unique
	private List<FormattedCharSequence> motd;

	@Inject(method = "render", at = @At("HEAD"))
	private void slo$initMotd(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f, CallbackInfo ci) {
		if (summary instanceof ServerLevelSummary serverLevelSummary && serverLevelSummary.extendedDirectory.slo$showMotd() && serverLevelSummary.extendedDirectory.slo$motd() != null) {
			motd = minecraft.font.split(Component.literal(serverLevelSummary.extendedDirectory.slo$motd()), l - 32 - 2);
		}
	}

	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", ordinal = 1))
	private boolean slo$renderMotd1(GuiGraphics instance, Font font, String string, int i, int j, int k, boolean bl) {
		if (motd == null) {
			return true;
		}
		if (!motd.isEmpty()) {
			instance.drawString(font, motd.getFirst(), i, j, k, bl);
		}
		return false;
	}

	@WrapWithCondition(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"))
	private boolean slo$renderMotd2(GuiGraphics instance, Font font, Component component, int i, int j, int k, boolean bl) {
		if (motd == null) {
			return true;
		}
		if (motd.size() >= 2) {
			instance.drawString(font, motd.get(1), i, j, k, bl);
		}
		return false;
	}

	@Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
	private void slo$joinServerWorld(CallbackInfo ci) {
		if (summary.primaryActionActive() && summary instanceof ServerLevelSummary serverLevelSummary) {
			ci.cancel();
			try {
				Slo.load(minecraft, screen, serverLevelSummary.extendedDirectory);
			}
			catch (IOException e) {
				Slo.LOGGER.error("Failed to join server world '{}'", serverLevelSummary.extendedDirectory.slo$directory().directoryName(), e);
			}
		}
	}

	@Inject(method = "recreateWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;createFromExisting(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/world/level/LevelSettings;Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext;Ljava/nio/file/Path;)Lnet/minecraft/client/gui/screens/worldselection/CreateWorldScreen;"))
	private void slo$recreateServerWorld(CallbackInfo ci) {
		if (summary instanceof ServerLevelSummary serverLevelSummary) {
			Slo.createFromExisting = serverLevelSummary.extendedDirectory;
		}
	}
}
