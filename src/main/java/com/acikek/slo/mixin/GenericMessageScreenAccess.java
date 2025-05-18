package com.acikek.slo.mixin;

import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GenericMessageScreen.class)
public interface GenericMessageScreenAccess {

    @Accessor
    FocusableTextWidget getTextWidget();
}
