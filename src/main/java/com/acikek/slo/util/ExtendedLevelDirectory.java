package com.acikek.slo.util;

import java.io.IOException;
import java.util.List;

public interface ExtendedLevelDirectory {

    boolean slo$isServer();

    String slo$jarPath();

    List<String> slo$processArgs();

    String slo$levelName();

    void slo$setLevelName(String levelName) throws IOException;
}
