package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class InstanceHelper {

    private static Logger logger = LoggerFactory.getLogger(InstanceHelper.class);

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
        launchers.forEach(s -> logger.info("Detected running bootstrapper: " + s));
        int launcherCount = Settings.getInstance().isProduction() ? launchers.size() : launchers.size() + 1;
        return launcherCount > 1;
    }

    public static boolean checkForOtherPdxuInstances(String[] args) {
        var app = ProcessHandle.allProcesses()
                .filter(h -> h.info().command().orElse("").startsWith(
                        Settings.getInstance().getAppInstallPath().resolve(
                                Path.of("app", "bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        app.forEach(s -> logger.info("Detected running app: " + s));
        if (app.size() > 0) {
            if (args.length == 0 && showKillInstanceDialog()) {
                app.forEach(ProcessHandle::destroyForcibly);
                return true;
            }

            return false;
        }
        return true;

    }
}
