package com.acikek.slo.util;

import java.io.IOException;
import java.util.List;

public interface ExtendedLevelDirectory {

    boolean slo$isServer();

    void slo$setJarPath(String jarPath);

    List<String> slo$jarCandidates();

    List<String> slo$processArgs();

    String slo$levelName();

    void slo$setLevelName(String levelName);

    boolean slo$autoScreenshot();

    String slo$motd();

    void slo$writeProperties() throws IOException;
}
