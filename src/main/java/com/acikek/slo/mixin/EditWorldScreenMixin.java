package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
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

    @Shadow @Final private LevelStorageSource.LevelStorageAccess levelAccess;

    @ModifyArg(method = "<init>", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button;builder(Lnet/minecraft/network/chat/Component;Lnet/minecraft/client/gui/components/Button$OnPress;)Lnet/minecraft/client/gui/components/Button$Builder;", ordinal = 3))
    private Button.OnPress slo$openServerDirectory(Button.OnPress onPress) {
        var extended = (ExtendedLevelDirectory) (Object) levelAccess.getLevelDirectory();
        return extended != null && extended.slo$isServer()
                ? button -> Util.getPlatform().openPath(levelAccess.getLevelDirectory().path())
                : onPress;
    }
}
