package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorPreset;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(WorldCreationUiState.class)
public abstract class WorldCreationUiStateMixin {

    @Shadow @Final private List<WorldCreationUiState.WorldTypeEntry> normalPresetList;

    @Shadow @Final private List<WorldCreationUiState.WorldTypeEntry> altPresetList;

    @Inject(method = "updatePresetLists", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState$WorldTypeEntry;preset()Lnet/minecraft/core/Holder;"))
    private void slo$addPresets(CallbackInfo ci) {
        if (Slo.worldPresets.isEmpty()) {
            return;
        }
        var entries = Slo.worldPresets.keySet().stream()
                .map(presetName -> {
                    var safeName = Util.sanitizeName(presetName, ResourceLocation::validPathChar);
                    var resourceKey = ResourceKey.create(Registries.WORLD_PRESET, ResourceLocation.fromNamespaceAndPath(Slo.MOD_ID, safeName));
                    var holder = Holder.Reference.createStandAlone(null, resourceKey);
                    return new WorldCreationUiState.WorldTypeEntry(holder);
                })
                .toList();
        normalPresetList.addAll(entries);
        altPresetList.addAll(entries);
    }

    @WrapWithCondition(method = "setWorldType", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;updateDimensions(Lnet/minecraft/client/gui/screens/worldselection/WorldCreationContext$DimensionsUpdater;)V"))
    private boolean slo$stopDimensionUpdate(WorldCreationUiState instance, WorldCreationContext.DimensionsUpdater dimensionsUpdater, @Local Holder<WorldPreset> holder) {
        return !holder.unwrapKey().map(key -> key.location().getNamespace().equals(Slo.MOD_ID)).orElse(false);
    }
}
