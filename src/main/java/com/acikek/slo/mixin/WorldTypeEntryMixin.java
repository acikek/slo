package com.acikek.slo.mixin;

import com.acikek.slo.Slo;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Optional;
import java.util.function.Function;

@Mixin(WorldCreationUiState.WorldTypeEntry.class)
public class WorldTypeEntryMixin {

    @Shadow @Final private @Nullable Holder<WorldPreset> preset;

    @ModifyArg(method = "describePreset", at = @At(value = "INVOKE", target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Function<? super ResourceKey<WorldPreset>, ? extends Component> slo$fallbackTranslation(Function<? super @NotNull ResourceKey<WorldPreset>, ? extends Component> mapper) {
        if (Optional.ofNullable(preset).flatMap(Holder::unwrapKey).map(key -> key.location().getNamespace().equals(Slo.MOD_ID)).orElse(false)) {
            return resourceKey -> Component.translatableWithFallback(
                    resourceKey.location().toLanguageKey("generator"),
                    Slo.worldPresets.get(resourceKey.location().getPath()).directoryName()
            );
        }
        return mapper;
    }
}
