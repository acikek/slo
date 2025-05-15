package com.acikek.slo.util;

import java.io.IOException;

public interface ExtendedLevelDirectory {

    boolean slo$isServer();

    String slo$jarPath();

    String slo$levelName();

    void slo$setLevelName(String levelName) throws IOException;
}
