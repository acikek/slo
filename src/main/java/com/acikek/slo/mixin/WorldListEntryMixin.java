package com.acikek.slo.mixin;

import com.acikek.slo.screen.SelectJarCandidateScreen;
import com.acikek.slo.screen.LoadServerLevelScreen;
import com.acikek.slo.util.ServerLevelSummary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldListEntryMixin {

    @Shadow @Final LevelSummary summary;

    @Shadow @Final private Minecraft minecraft;

    @Shadow @Final private SelectWorldScreen screen;

    @Inject(method = "joinWorld", at = @At("HEAD"), cancellable = true)
    private void slo$joinServerWorld(CallbackInfo ci) throws IOException {
        if (summary.primaryActionActive() && summary instanceof ServerLevelSummary serverLevelSummary) {
            ci.cancel();
            if (serverLevelSummary.extendedDirectory.slo$jarCandidates() != null) {
                minecraft.setScreen(new SelectJarCandidateScreen(screen, serverLevelSummary));
            }
            else {
                LoadServerLevelScreen.load(minecraft, screen, serverLevelSummary);
            }
        }
    }
}
