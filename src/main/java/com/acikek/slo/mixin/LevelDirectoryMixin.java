package com.acikek.slo.mixin;

import com.acikek.slo.util.ExtendedLevelDirectory;
import com.acikek.slo.Slo;
import com.google.common.hash.Hashing;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Mixin(LevelStorageSource.LevelDirectory.class)
public abstract class LevelDirectoryMixin implements ExtendedLevelDirectory {

    @Unique
    private static final ResourceLocation MISSING_ICON = ResourceLocation.withDefaultNamespace("textures/misc/unknown_server.png");

    @Shadow @Final
    Path path;

    @Shadow public abstract String directoryName();

    @Shadow public abstract Path iconFile();

    @Unique
    private boolean server;

    @Unique
    private Properties serverProperties = new Properties();

    @Unique
    private Properties sloProperties;

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

    @Unique
    private String levelName;

    @Unique
    private boolean autoScreenshot;

    @Unique
    private boolean showMotd;

    @Unique
    private String motd;

    @Unique
    private ResourceLocation levelType;

    @Unique
    private boolean triedLoadIcon;

    @Unique
    private ResourceLocation iconTexture;

    // TODO: stop throw
    @Inject(method = "<init>", at = @At("TAIL"))
    private void slo$init(Path path, CallbackInfo ci) {
        try {
            var serverPropertiesPath = path.resolve("server.properties");
            if (Files.isRegularFile(serverPropertiesPath)) {
                try (var reader = new FileReader(serverPropertiesPath.toFile())) {
                    serverProperties.load(reader);
                }
            }
            if (!(server = slo$tryInitServer())) {
                return;
            }
            motd = serverProperties.getProperty("motd");
            var levelTypeStr = serverProperties.getProperty("level-type");
            if (levelTypeStr != null) {
                levelType = ResourceLocation.tryParse(levelTypeStr);
            }
            if (Slo.directoryInitUpdate) {
                slo$writeAcceptedEula();
            }
        }
        catch (IOException e) {
            Slo.LOGGER.error("Failed to initialize server level directory", e);
        }
    }

    @Unique
    private boolean slo$tryInitServer() throws IOException {
        return slo$tryInitFromConfig() || (Slo.directoryInitAutodetect && slo$initFromAutodetect());
    }

    @Unique
    private boolean slo$tryInitFromConfig() throws IOException {
        var configPath = path.resolve("slo.properties");
        if (!Files.isRegularFile(configPath)) {
            return false;
        }
        try (var reader = new FileReader(configPath.toFile())) {
            sloProperties = new Properties();
            sloProperties.load(reader);
            slo$readProperties(sloProperties);
            if (jarPath == null) {
                Slo.LOGGER.error("Server level '{}' missing required configuration property 'jar-path'", directoryName());
            }
            if (Slo.directoryInitUpdate) {
                slo$writeSloProperties();
            }
            return true;
        }
    }

    @Unique
    private boolean slo$initFromAutodetect() throws IOException {
        var jarFiles = path.toFile().listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            return false;
        }
        sloProperties = new Properties();
        if (jarFiles.length == 1) {
            var jarPath = jarFiles[0].getName();
            sloProperties.setProperty("jar-path", jarPath);
            Slo.LOGGER.info("Autodetected jar '{}' in server level '{}'", jarPath, directoryName());
        }
        else {
            jarCandidates = Arrays.stream(jarFiles).map(File::getName).toList();
            Slo.LOGGER.info("Found {} potential jars in server level '{}': {}", jarCandidates.size(), directoryName(), String.join(", ", jarCandidates));
        }
        slo$readProperties(sloProperties);
        if (jarFiles.length == 1 && Slo.directoryInitUpdate) {
            slo$writeSloProperties();
        }
        return true;
    }

    @Unique
    private void slo$readProperties(Properties properties) {
        jvmOptions = properties.getProperty("jvm-options", "");
        jarPath = properties.getProperty("jar-path");
        jarArgs = properties.getProperty("jar-args", "--nogui");
        resourcePath = properties.getProperty("resource-path", "world");
        levelName = properties.getProperty("level-name");
        autoScreenshot = properties.getProperty("auto-screenshot", "false").equals("true");
        showMotd = properties.getProperty("show-motd", "false").equals("true");
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
    public LevelStorageSource.LevelDirectory slo$directory() {
        return (LevelStorageSource.LevelDirectory) (Object) this;
    }

    @Override
    public boolean slo$isServer() {
        return server;
    }

    @Override
    public Properties slo$serverProperties() {
        return serverProperties;
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
        return levelName != null ? levelName : directoryName();
    }

    @Override
    public void slo$setLevelName(String levelName) {
        this.levelName = levelName;
    }

    @Override
    public boolean slo$autoScreenshot() {
        return autoScreenshot;
    }

    @Override
    public boolean slo$showMotd() {
        return showMotd;
    }

    @Override
    public String slo$motd() {
        return motd;
    }

    @Override
    public ResourceLocation slo$levelType() {
        return levelType;
    }

    @Unique
    private ResourceLocation slo$tryLoadIcon() {
        if (!Files.isRegularFile(iconFile())) {
            return null;
        }
        try (var stream = new FileInputStream(iconFile().toFile())) {
            var nativeImage = NativeImage.read(stream);
            var hashedPath = Util.sanitizeName(directoryName(), ResourceLocation::validPathChar) + "/" + Hashing.sha1().hashUnencodedChars(directoryName()) + "/icon";
            var texture = ResourceLocation.fromNamespaceAndPath(Slo.MOD_ID, "preset/" + hashedPath);
            Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(texture::toString, nativeImage));
            return texture;
        }
        catch (IOException e) {
            Slo.LOGGER.warn("Failed to load icon for preset '{}'", directoryName(), e);
        }
        return null;
    }

    @Override
    public ResourceLocation slo$loadIconTexture() {
        if (triedLoadIcon) {
            return iconTexture;
        }
        iconTexture = slo$tryLoadIcon();
        if (iconTexture == null) {
            iconTexture = MISSING_ICON;
        }
        return iconTexture;
    }

    @Unique
    public void slo$writeSloProperties() throws IOException {
        sloProperties.setProperty("jvm-options", jvmOptions);
        sloProperties.setProperty("jar-path", jarPath);
        sloProperties.setProperty("jar-args", jarArgs);
        sloProperties.setProperty("resource-path", resourcePath);
        if (levelName != null) {
            sloProperties.setProperty("level-name", levelName);
        }
        sloProperties.setProperty("auto-screenshot", autoScreenshot ? "true" : "false");
        sloProperties.setProperty("show-motd", showMotd ? "true" : "false");
        sloProperties.store(new FileWriter(path.resolve("slo.properties").toFile()), null);
    }

    @Override
    public void slo$writeServerProperties() throws IOException {
        serverProperties.store(new FileWriter(path.resolve("server.properties").toFile()), null);
    }

    @Unique
    public void slo$writeAcceptedEula() throws IOException {
        try (var writer = new FileWriter(path.resolve("eula.txt").toFile())) {
            writer.write("eula=true");
        }
    }
}
