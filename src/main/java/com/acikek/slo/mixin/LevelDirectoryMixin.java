package com.acikek.slo.mixin;

import com.acikek.slo.ExtendedLevelDirectory;
import com.acikek.slo.Slo;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

@Mixin(LevelStorageSource.LevelDirectory.class)
public abstract class LevelDirectoryMixin implements ExtendedLevelDirectory {

    @Shadow @Final
    Path path;

    @Shadow public abstract String directoryName();

    @Unique
    private boolean server;

    //@Unique
    //private String jvmOptions;

    @Unique
    private String jarPath;

    //private String jarArgs;

    @Unique
    private String resourcePath;

    @Unique
    private String levelName;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void a(Path path, CallbackInfo ci) throws IOException {
        var propertiesFile = path.resolve("slo.properties").toFile();
        if (propertiesFile.exists()) {
            try (var reader = new FileReader(propertiesFile)) {
                var properties = new Properties();
                properties.load(reader);
                slo$initProperties(properties);
            }
        }
    }

    @Unique
    private void slo$initProperties(Properties properties) {
        jarPath = properties.getProperty("jar-path");
        if (jarPath == null) {
            Slo.LOGGER.error("Server world '{}' missing required configuration property 'jar-path'", directoryName());
            return;
        }
        resourcePath = properties.getProperty("resource-path", "world");
        levelName = properties.getProperty("level-name", path.getFileName().toString());
        server = true;
    }

    @Inject(method = "iconFile", at = @At(value = "HEAD"), cancellable = true)
    private void c(CallbackInfoReturnable<Path> cir) {
        if (server) {
            cir.setReturnValue(path.resolve("server-icon.png"));
        }
    }

    @ModifyExpressionValue(method = "resourcePath", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;path:Ljava/nio/file/Path;"))
    private Path b(Path original) {
        return resourcePath != null ? original.resolve(resourcePath) : original;
    }

    @Override
    public boolean slo$isServer() {
        return server;
    }

    @Override
    public String slo$jarPath() {
        return jarPath;
    }

    @Override
    public String slo$levelName() {
        return levelName;
    }
}
