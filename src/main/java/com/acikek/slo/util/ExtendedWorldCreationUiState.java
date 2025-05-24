package com.acikek.slo.util;

import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

public interface ExtendedWorldCreationUiState {

    ExtendedLevelDirectory slo$presetDirectory();

    void slo$setPresetDirectory(ExtendedLevelDirectory presetDirectory);

    void slo$setWorldTypeButton(CycleButton<WorldCreationUiState.WorldTypeEntry> worldTypeButton);

    void slo$updateWorldTypeButton();
}
