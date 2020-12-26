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

    public static boolean run() {
        boolean doUpdate = Settings.getInstance().autoupdateEnabled() && Settings.getInstance().updateLauncher();
        logger.info("Doing launcher update: " + doUpdate);
        if (!doUpdate) {
            return true;
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
            return true;
        }

        logger.info("Download info: " + info.toString());

        boolean reqUpdate = Settings.getInstance().forceUpdate() ||
                !info.version.equals(Settings.getInstance().getVersion());
        if (!reqUpdate) {
            logger.info("No launcher update required");
            return true;
        }

        UpdaterGui frame = new UpdaterGui();
        frame.setVisible(true);
        var l = Settings.getInstance().getLauncherInstallPath();
        logger.info("Downloading " + info.url.toString());
        try {
            Path pathToNewest = downloadFile(info.url, frame::setProgress);
            frame.dispose();
            if (SystemUtils.IS_OS_WINDOWS) {
                new ProcessBuilder(
                        Settings.getInstance().getElevatePath().toString(),
                        "msiexec",
                        "/qn",
                        "/i", pathToNewest.toString(),
                        "/log", Settings.getInstance().getLogsPath().resolve("installer_" + info.version + ".log").toString(),
                        l.map(p -> "INSTALLDIR=" + p.toString()).orElse("")).start();
            }
            return false;
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        frame.dispose();
        return true;
    }
}
