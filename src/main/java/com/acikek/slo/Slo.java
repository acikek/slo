package com.acikek.slo;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Slo implements ModInitializer {

	public static final String MOD_ID = "slo";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final String JAVA_PATH = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";

	public static Process serverProcess;

	@Override
	public void onInitialize() {
		// SymlinkLevelSummary?
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			serverProcess.destroy();
		}));
	}
}
