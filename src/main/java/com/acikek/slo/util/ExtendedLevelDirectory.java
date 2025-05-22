package com.acikek.slo.util;

import com.acikek.slo.Slo;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface ExtendedLevelDirectory {

    LevelStorageSource.LevelDirectory slo$directory();

    boolean slo$isServer();

    void slo$setJarPath(String jarPath);

    List<String> slo$jarCandidates();

    List<String> slo$processArgs();

    String slo$levelName();

    void slo$setLevelName(String levelName);

    boolean slo$autoScreenshot();

    String slo$motd();

    void slo$writeProperties() throws IOException;

    static ExtendedLevelDirectory create(Path path, boolean update, boolean autodetect) {
        Slo.directoryInitUpdate = update;
        Slo.directoryInitAutodetect = autodetect;
        var directory = new LevelStorageSource.LevelDirectory(path);
        Slo.directoryInitUpdate = Slo.directoryInitAutodetect = false;
        return (ExtendedLevelDirectory) (Object) directory;
    }
}
