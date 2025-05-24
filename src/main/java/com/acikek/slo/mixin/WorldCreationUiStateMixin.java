package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.util.ExtendedWorldCreationUiState;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin implements ExtendedWorldCreationUiState {

    @Shadow public abstract List<WorldCreationUiState.WorldTypeEntry> getAltPresetList();

    @Unique
    private ExtendedLevelDirectory presetDirectory;

    @Unique
    private CycleButton<WorldCreationUiState.WorldTypeEntry> worldTypeButton;

    @Unique
    private WorldCreationUiState.WorldTypeEntry previousWorldType;

    @Override
    public ExtendedLevelDirectory slo$presetDirectory() {
        return presetDirectory;
    }

    @Override
    public void slo$setPresetDirectory(ExtendedLevelDirectory presetDirectory) {
        this.presetDirectory = presetDirectory;
    }

    @Override
    public void slo$setWorldTypeButton(CycleButton<WorldCreationUiState.WorldTypeEntry> worldTypeButton) {
        this.worldTypeButton = worldTypeButton;
    }

    @Override
    public void slo$updateWorldTypeButton() {
        if (slo$presetDirectory() != null && slo$presetDirectory().slo$levelType() != null) {
            for (var entry : getAltPresetList()) {
                if (entry.preset() != null && entry.preset().unwrapKey().map(key -> key.location().equals(presetDirectory.slo$levelType())).orElse(false)) {
                    previousWorldType = worldTypeButton.getValue();
                    worldTypeButton.setValue(entry);
                    worldTypeButton.active = false;
                    return;
                }
            }
        }
        if (previousWorldType != null) {
            worldTypeButton.setValue(previousWorldType);
            previousWorldType = null;
            worldTypeButton.active = true;
        }
    }
}
