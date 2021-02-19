package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.crschnick.pdx_unlimiter.updater.DirectoryHelper.*;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.downloadFile;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.getInfo;

public class Eu4SeUpdater {

    private static Logger logger = LoggerFactory.getLogger(Eu4SeUpdater.class);

    public static void update() {
        boolean enabled = Settings.getInstance().eu4EditorEnabled();
        Path target = Settings.getInstance().getAppInstallPath().resolveSibling("Eu4SaveEditor");

        logger.info("Eu4SaveEditor enabled: " + enabled);
        if (!enabled) {
            if (Files.exists(target)) {
                try {
                    logger.info("Deleting directory: " + target.toString());
                    FileUtils.deleteDirectory(target.toFile());
                } catch (IOException e) {
                    ErrorHandler.handleException(e);
                }
            }
            return;
        }

        GithubHelper.DownloadInfo info;
        try {
            info = getInfo(
                    new URL("https://github.com/Osallek/Eu4SaveEditor/releases/latest/download/"),
                    "Eu4SaveEditor",
                    "zip",
                    true);

        } catch (Exception e) {
            ErrorHandler.handleException(e);
            return;
        }

        logger.info("Download info: " + info.toString());

        boolean reqUpdate = (Settings.getInstance().forceUpdate() ||
                !info.version.equals(Settings.getInstance().getVersion()));
        if (!reqUpdate) {
            logger.info("No Eu4SaveEditor update required");
            return;
        }

        UpdaterGui frame = new UpdaterGui();
        try {
            logger.info("Downloading " + info.url.toString());
            Path pathToNewest = downloadFile(info.url, frame::setProgress, frame::isDestroyed);
            logger.info("Download complete");
            if (pathToNewest == null) {
                logger.info("Update skipped by user");
                return;
            }
            logger.info("Deleting old version");
            deleteOldVersion(target);
            logger.info("Unzipping new version");
            unzip(pathToNewest, target);
            logger.info("Writing version");
            writeVersion(target, info.version);
            logger.info("Update completed for Eu4SaveEditor");
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        frame.dispose();
    }
}
