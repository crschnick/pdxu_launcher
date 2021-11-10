package com.crschnick.pdx_unlimiter.updater.util;

import com.crschnick.pdx_unlimiter.updater.ErrorHandler;
import com.crschnick.pdx_unlimiter.updater.Settings;
import com.crschnick.pdx_unlimiter.updater.Updater;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class InstanceHelper {

    private static final Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

    private static boolean showKillInstanceDialog() {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(Updater.class.getResource("logo.png")).getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        int r = JOptionPane.showConfirmDialog(null, """
                It seems like there is already a Pdx-Unlimiter instance running.
                If this is not intended, you can kill the running instance now by clicking 'yes'.
                                        """, "Pdx-Unlimiter", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon);
        return r == JOptionPane.YES_OPTION;
    }

    public static boolean areOtherLaunchersRunning() {
        Path runPath = Path.of(System.getProperty("java.home"));
        var launcherExecutable = runPath.getParent().resolve("Pdx-Unlimiter" +
                (SystemUtils.IS_OS_WINDOWS ? ".exe" : ""));
        var launchers = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.startsWith(launcherExecutable.toString()))
                .collect(Collectors.toList());
        launchers.forEach(s -> logger.info("Detected running launcher: " + s));
        int launcherCount = Settings.getInstance().isProduction() ? launchers.size() : launchers.size() + 1;
        return launcherCount > 1;
    }


    public static boolean shouldUpdateLauncher(String[] args) {
        if (!Settings.getInstance().autoupdateEnabled()) {
            logger.debug("Autoupdate disabled");
            return false;
        }

        if (!Settings.getInstance().updateLauncher()) {
            logger.debug("Dev disabled updates");
            return false;
        }

        if (args.length > 0) {
            logger.debug("Passing arguments");
            return false;
        }

        return true;
    }

    public static boolean shouldUpdateApp(String[] args) {
        // First install
        if (!Files.exists(Settings.getInstance().getAppInstallPath())) {
            logger.debug("Detected first install");
            return true;
        }

        if (!Settings.getInstance().autoupdateEnabled()) {
            logger.debug("Autoupdate disabled");

            if (Settings.getInstance().isErrorExit()) {
                logger.debug("However, error exit happened, therefore overriding autoupdate setting");
            } else {
                return false;
            }
        }

        Path pdxuExecutable = SystemUtils.IS_OS_WINDOWS ? Path.of("app", "Pdx-Unlimiter.exe") :
                Path.of("app", "bin", "Pdx-Unlimiter");
        var app = ProcessHandle.allProcesses()
                .filter(h -> h.info().command().orElse("").startsWith(
                        Settings.getInstance().getAppInstallPath().resolve(pdxuExecutable).toString()))
                .collect(Collectors.toList());
        app.forEach(s -> logger.info("Detected running app: " + s));
        if (app.size() == 0) {
            return true;
        }

        // Just open file with existing pdxu instance, so don't update
        if (args.length > 0) {
            return false;
        }

        // Otherwise, try to kill existing instance(s)
        boolean shouldKill = showKillInstanceDialog();
        if (shouldKill) {
            for (ProcessHandle a : app) {
                boolean killed = a.destroyForcibly();
                if (!killed) {
                    ErrorHandler.handleException(
                            new IOException("Could not kill running Pdx-Unlimiter process with pid " + a.pid()));
                    return false;
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            for (ProcessHandle a : app) {
                if (a.isAlive()) {
                    logger.debug("An instance is still alive");
                    return false;
                }
            }

            logger.debug("Killed instances");
            return true;
        } else {
            logger.debug("Chose not to kill instances");
            return false;
        }
    }
}
