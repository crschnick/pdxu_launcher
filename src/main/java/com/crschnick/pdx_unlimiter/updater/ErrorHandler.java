package com.crschnick.pdx_unlimiter.updater;

import io.sentry.Sentry;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ErrorHandler {

    private static Logger logger;

    public static void handleException(Exception e) {
        Sentry.capture(e);
        logger.error("Error occured", e);
    }

    public static void init() {
        System.setProperty("sentry.dsn", "https://f86d7649617d4c9cb95db5a19811305b@o462618.ingest.sentry.io/5468640");
        System.setProperty("sentry.stacktrace.hidecommon", "false");
        System.setProperty("sentry.stacktrace.app.packages", "");
        System.setProperty("sentry.uncaught.handler.enabled", "true");
        System.setProperty("sentry.servername", "");

        try {
            FileUtils.forceMkdir(Settings.getInstance().getLogsPath().toFile());
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }

        if (Settings.getInstance().isProduction()) {
            var l = Settings.getInstance().getLogsPath().resolve("launcher.log");
            System.setProperty("org.slf4j.simpleLogger.logFile", l.toString());
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            System.setProperty("sentry.environment", "production");
            System.setProperty("sentry.release", Settings.getInstance().getVersion());
        } else {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("sentry.environment", "dev");
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        logger = LoggerFactory.getLogger(Updater.class);

        logger.info("Initializing with " + "production: " + Settings.getInstance().isProduction());
        Sentry.init();
    }
}
