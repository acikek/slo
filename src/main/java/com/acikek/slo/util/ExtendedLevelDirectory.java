package com.acikek.slo.util;

import com.acikek.slo.Slo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

public interface ExtendedLevelDirectory {

    LevelStorageSource.LevelDirectory slo$directory();

    boolean slo$isServer();

    Properties slo$serverProperties();

    void slo$setJarPath(String jarPath);

    List<String> slo$jarCandidates();

    List<String> slo$processArgs();

    String slo$levelName();

    void slo$setLevelName(String levelName);

    boolean slo$autoScreenshot();

    String slo$motd();

    void slo$loadIconTexture();

    ResourceLocation slo$iconTexture();

    void slo$writeSloProperties() throws IOException;

    void slo$writeServerProperties() throws IOException;

    static ExtendedLevelDirectory create(Path path, boolean update, boolean autodetect) {
        Slo.directoryInitUpdate = update;
        Slo.directoryInitAutodetect = autodetect;
        var directory = new LevelStorageSource.LevelDirectory(path);
        Slo.directoryInitUpdate = Slo.directoryInitAutodetect = true;
        return (ExtendedLevelDirectory) (Object) directory;
    }
}
