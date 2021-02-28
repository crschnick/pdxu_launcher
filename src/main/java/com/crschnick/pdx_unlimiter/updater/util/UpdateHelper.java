package com.crschnick.pdx_unlimiter.updater.util;

import com.crschnick.pdx_unlimiter.updater.ErrorHandler;
import com.crschnick.pdx_unlimiter.updater.gui.ChangelogGui;
import com.crschnick.pdx_unlimiter.updater.gui.UpdaterGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

import static com.crschnick.pdx_unlimiter.updater.util.DirectoryHelper.*;
import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.downloadFile;

public class UpdateHelper {

    private static final Logger logger = LoggerFactory.getLogger(UpdateHelper.class);

    public static void update(String title, Path target, GithubHelper.DownloadInfo info) {
        UpdaterGui frame = new UpdaterGui(title);
        frame.setVisible(true);

        ChangelogGui.displayChangelog(title, info.changelogUrl);

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
            logger.info("Update completed for " + title);
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        frame.dispose();
    }
}
