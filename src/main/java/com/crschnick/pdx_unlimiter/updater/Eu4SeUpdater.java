package com.crschnick.pdx_unlimiter.updater;

import com.crschnick.pdx_unlimiter.updater.util.DirectoryHelper;
import com.crschnick.pdx_unlimiter.updater.util.GithubHelper;
import com.crschnick.pdx_unlimiter.updater.util.UpdateHelper;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.getInfo;

public class Eu4SeUpdater {

    private static final Logger logger = LoggerFactory.getLogger(Eu4SeUpdater.class);

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
            logger.info("Download info: " + info.toString());

            boolean reqUpdate = (Settings.getInstance().forceUpdate() ||
                    DirectoryHelper.getVersion(target).map(v -> v.equals(info.version)).orElse(true));
            if (!reqUpdate) {
                logger.info("No Eu4SaveEditor update required");
                return;
            }
        } catch (Exception e) {
            ErrorHandler.handleException(e);
            return;
        }

        UpdateHelper.update("Eu4SaveEditor", target, info);
    }
}
