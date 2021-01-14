package com.crschnick.pdx_unlimiter.updater;

import io.sentry.Attachment;
import io.sentry.Sentry;
import io.sentry.SentryOptions;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ErrorHandler {

    private static Logger logger;

    private static boolean showErrorDialog(String msg) {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(ErrorHandler.class.getResource("logo.png"))
                    .getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {
        }

        int r = JOptionPane.showConfirmDialog(null, "Error occured while launching: " + msg +
                        "\n\nDo you want to automatically report this error to the developers?" +
                        "\nThis will send some diagnostics data.",
                "Pdx-Unlimiter", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, icon);
        return r == JOptionPane.YES_OPTION;
    }

    public static void handleException(Throwable e) {
        if (logger != null) {
            logger.error("Error occured while launching", e);
            if (showErrorDialog(e.getMessage())) {
                Sentry.configureScope(scope -> {
                    var l = Settings.getInstance().getLogsPath().resolve("launcher.log");
                    scope.addAttachment(new Attachment(l.toString()));
                });
                Sentry.captureException(e);
            }
        } else {
            e.printStackTrace();
        }
    }

    public static void init() {
        try {
            FileUtils.forceMkdir(Settings.getInstance().getLogsPath().toFile());
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }

        SentryOptions opts = new SentryOptions();
        opts.setServerName(System.getProperty("os.name"));
        opts.setDsn("https://f86d7649617d4c9cb95db5a19811305b@o462618.ingest.sentry.io/5468640");
        if (Settings.getInstance().isProduction()) {
            var l = Settings.getInstance().getLogsPath().resolve("launcher.log");
            System.setProperty("org.slf4j.simpleLogger.logFile", l.toString());
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            opts.setEnvironment("production");
            opts.setRelease(Settings.getInstance().getVersion());
        } else {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            opts.setEnvironment("dev");
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        logger = LoggerFactory.getLogger(ErrorHandler.class);

        logger.info("Initializing with " + "production: " + Settings.getInstance().isProduction());

        Sentry.init(opts);
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            handleException(e);
        });
    }
}
