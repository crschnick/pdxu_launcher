package com.crschnick.pdx_unlimiter.updater;

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

        }
        int r = JOptionPane.showConfirmDialog(null, """
                It seems like there is already a Pdx-Unlimiter instance running.
                If this is not intended, you can kill the running instance now by clicking 'yes'.
                                        """, "Pdx-Unlimiter", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, icon);
        return r == JOptionPane.YES_OPTION;
    }

    public static boolean checkForOtherPdxuInstances() {
        var launcher = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.startsWith(Settings.getInstance().getInstallPath().resolve(
                        Path.of("launcher", "bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        launcher.forEach(s -> logger.info("Detected running launcher: " + s));
        int launcherCount = Settings.getInstance().isProduction() ? launcher.size() : launcher.size() + 1;

        if (Settings.getInstance().isBootstrap()) {
            Path runPath = Path.of(System.getProperty("java.home"));
            var bootsrapExecutable = runPath.getParent().resolve("Pdx-Unlimiter.exe");
            var bootstrappers = ProcessHandle.allProcesses()
                    .map(h -> h.info().command().orElse(""))
                    .filter(s -> s.startsWith(bootsrapExecutable.toString()))
                    .collect(Collectors.toList());
            bootstrappers.forEach(s -> logger.info("Detected running bootstrapper: " + s));
            int count = Settings.getInstance().isProduction() ? bootstrappers.size() : bootstrappers.size() + 1;

            // Starting launcher initially and no other bootstrapper instance is running.
            return launcherCount == 0 && count == 1;
        } else {
            if (launcherCount > 1) {
                return false;
            }

            var app = ProcessHandle.allProcesses()
                    .filter(h -> h.info().command().orElse("").startsWith(Settings.getInstance().getInstallPath().resolve(
                            Path.of("app", "bin", "java.exe")).toString()))
                    .collect(Collectors.toList());
            app.forEach(s -> logger.info("Detected running app: " + s));

            if (app.size() > 0) {
                boolean shouldKill = showKillInstanceDialog();
                if (shouldKill) {
                    app.forEach(ProcessHandle::destroyForcibly);
                    return true;
                }

                return false;
            }
            return true;
        }
    }
}
