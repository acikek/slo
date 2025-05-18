package com.acikek.slo.util;

import com.acikek.slo.Slo;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;
import net.minecraft.world.level.storage.LevelVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class ServerLevelSummary extends LevelSummary {

    public LevelStorageSource.LevelDirectory directory;
    public ExtendedLevelDirectory extendedDirectory;

    public ServerLevelSummary(LevelSettings levelSettings, LevelVersion levelVersion, String levelId, boolean requiresManualConversion, boolean locked, boolean experimental, Path icon, LevelStorageSource.LevelDirectory directory) {
        super(levelSettings, levelVersion, levelId, requiresManualConversion, locked, experimental, icon);
        this.directory = directory;
        this.extendedDirectory = (ExtendedLevelDirectory) (Object) directory;
    }

    @Override
    public @NotNull String getLevelName() {
        return extendedDirectory.slo$levelName();
    }

    @Override
    public boolean primaryActionActive() {
        return true; // TODO: check available port instead?
    }
}
