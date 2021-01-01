package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.file.Path;

import static com.crschnick.pdx_unlimiter.updater.GithubHelper.downloadFile;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.getInfo;

public class LauncherUpdater {

    private static Logger logger = LoggerFactory.getLogger(LauncherUpdater.class);

    public static boolean update() {
        boolean doUpdate = Settings.getInstance().autoupdateEnabled() && Settings.getInstance().updateLauncher();
        logger.info("Doing launcher update: " + doUpdate);
        if (!doUpdate) {
            return false;
        }

        GithubHelper.DownloadInfo info;
        try {
            info = getInfo(
                    new URL("https://github.com/crschnick/pdxu_installer/releases/latest/download/"),
                    "pdxu_installer",
                    SystemUtils.IS_OS_WINDOWS ? "msi" : "deb",
                    true);

        } catch (Exception e) {
            ErrorHandler.handleException(e);
            return false;
        }

        logger.info("Download info: " + info.toString());

        boolean reqUpdate = Settings.getInstance().forceUpdate() ||
                !info.version.equals(Settings.getInstance().getVersion());
        if (!reqUpdate) {
            logger.info("No launcher update required");
            return false;
        }

        UpdaterGui frame = new UpdaterGui();
        frame.setVisible(true);
        logger.info("Downloading " + info.url.toString());
        try {
            Path pathToNewest = downloadFile(info.url, frame::setProgress, frame::isDestroyed);
            frame.dispose();
            if (pathToNewest == null) {
                return false;
            }

            if (SystemUtils.IS_OS_WINDOWS) {
                var l = Settings.getInstance().getLauncherInstallPath();
                new ProcessBuilder(
                        Settings.getInstance().getElevatePath().toString(),
                        "msiexec",
                        "/qb+",
                        "/i", pathToNewest.toString(),
                        "/lv", Settings.getInstance().getLogsPath().resolve("installer_" + info.version + ".log").toString(),
                        l.map(p -> "INSTALLDIR=" + p.toString()).orElse("")).start();
            } else {
                var pw = new ProcessBuilder(
                        "/usr/bin/ssh-askpass",
                        "A Pdx-Unlimiter launcher update is available. " +
                                "To start it, your sudo password is required.")
                        .start();
                String pwString = new String(pw.getInputStream().readAllBytes()).replace("\n", "");

                var proc = new ProcessBuilder(
                        "/bin/sh",
                        "-c",
                        "echo " + pwString + " | sudo -S apt install " + pathToNewest.toString());
                proc.redirectErrorStream(true);
                proc.redirectOutput(Settings.getInstance().getLogsPath().resolve("installer_" + info.version + ".log").toFile());
                proc.start();
            }
            return true;
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        frame.dispose();
        return false;
    }
}
