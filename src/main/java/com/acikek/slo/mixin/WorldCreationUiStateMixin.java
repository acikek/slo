package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin implements ExtendedWorldCreationUiState {

    @Unique
    private ExtendedLevelDirectory presetDirectory;

    @Override
    public ExtendedLevelDirectory slo$presetDirectory() {
        return presetDirectory;
    }

    @Override
    public void slo$setPresetDirectory(ExtendedLevelDirectory presetDirectory) {
        this.presetDirectory = presetDirectory;
    }
}
