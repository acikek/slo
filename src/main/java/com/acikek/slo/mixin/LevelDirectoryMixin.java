package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Mixin(LevelStorageSource.LevelDirectory.class)
public abstract class LevelDirectoryMixin implements ExtendedLevelDirectory {

    @Shadow @Final
    Path path;

    @Shadow public abstract String directoryName();

    @Unique
    private boolean server;

    @Unique
    private Properties properties;

    @Unique
    private String jvmOptions;

    @Unique
    private String jarPath;

    @Unique
    private List<String> jarCandidates;

    @Unique
    private String jarArgs;

    @Unique
    private String resourcePath;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void slo$init(Path path, CallbackInfo ci) throws IOException {
        var propertiesFile = slo$propertiesFile();
        if (propertiesFile.exists()) {
            try (var reader = new FileReader(propertiesFile)) {
                properties = new Properties();
                properties.load(reader);
                slo$readProperties(properties);
                if (jarPath == null) {
                    Slo.LOGGER.error("Server world '{}' missing required configuration property 'jar-path'", directoryName());
                }
                server = true;
                return;
            }
        }
        if (!path.resolve("server.properties").toFile().exists()) {
            return;
        }
        var jarFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return;
        }
        properties = new Properties();
        if (jarFiles.length == 1) {
            var jarPath = jarFiles[0].getName();
            properties.setProperty("jar-path", jarPath);
            Slo.LOGGER.info("Autodetected jar '{}' in server level '{}'", jarPath, directoryName());
        }
        else {
            jarCandidates = Arrays.stream(jarFiles).map(File::getName).toList();
            Slo.LOGGER.info("Found {} potential jars in server level '{}': {}", jarCandidates.size(), directoryName(), String.join(", ", jarCandidates));
        }
        slo$readProperties(properties);
        if (jarFiles.length == 1) {
            slo$writeProperties();
        }
        server = true;
    }

    @Unique
    private void slo$readProperties(Properties properties) {
        jvmOptions = properties.getProperty("jvm-options", "");
        jarPath = properties.getProperty("jar-path");
        jarArgs = properties.getProperty("jar-args", "--nogui");
        resourcePath = properties.getProperty("resource-path", "world");
    }

    @Inject(method = "iconFile", at = @At(value = "HEAD"), cancellable = true)
    private void slo$modifyIconFile(CallbackInfoReturnable<Path> cir) {
        if (server) {
            cir.setReturnValue(path.resolve("server-icon.png"));
        }
    }

    @ModifyExpressionValue(method = "resourcePath", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelDirectory;path:Ljava/nio/file/Path;"))
    private Path slo$modifyResourcePath(Path original) {
        return resourcePath != null ? original.resolve(resourcePath) : original;
    }

    @Override
    public boolean slo$isServer() {
        return server;
    }

    @Override
    public void slo$setJarPath(String jarPath) {
        this.jarPath = jarPath;
        jarCandidates = null;
    }

    @Override
    public List<String> slo$jarCandidates() {
        return jarCandidates;
    }

    @Override
    public List<String> slo$processArgs() {
        List<String> result = new ArrayList<>();
        result.add(Slo.JAVA_PATH);
        Collections.addAll(result, jvmOptions.split(" "));
        Collections.addAll(result, "-jar", jarPath);
        Collections.addAll(result, jarArgs.split(" "));
        return result.stream().filter(str -> !str.isEmpty()).toList();
    }

    @Override
    public String slo$levelName() {
        return properties.getProperty("level-name", directoryName());
    }

    @Override
    public void slo$setLevelName(String levelName) {
        properties.setProperty("level-name", levelName);
    }

    @Unique
    private File slo$propertiesFile() {
        return path.resolve("slo.properties").toFile();
    }

    @Unique
    public void slo$writeProperties() throws IOException {
        properties.setProperty("jvm-options", jvmOptions);
        properties.setProperty("jar-path", jarPath);
        properties.setProperty("jar-args", jarArgs);
        properties.setProperty("resource-path", resourcePath);
        properties.store(new FileWriter(slo$propertiesFile()), null);
    }
}
