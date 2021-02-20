package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.crschnick.pdx_unlimiter.updater.DirectoryHelper.deleteOldVersion;
import static com.crschnick.pdx_unlimiter.updater.DirectoryHelper.unzip;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.downloadFile;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.getInfo;

public class AppUpdater {

    private static Logger logger = LoggerFactory.getLogger(AppUpdater.class);

    public static void run(String[] args) {
        boolean doUpdate = InstanceHelper.shouldUpdateApp(args);
        logger.info("Doing app update: " + doUpdate);
        if (doUpdate) {
            UpdaterGui frame = new UpdaterGui();
            try {
                update(frame,
                        new URL("https://github.com/crschnick/pdx_unlimiter/releases/latest/download/"),
                        "pdx_unlimiter",
                        "zip",
                        Settings.getInstance().getAppInstallPath().resolve("app"),
                        true);
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }

            try {
                update(frame,
                        new URL("https://github.com/crschnick/pdxu_rakaly/releases/latest/download/"),
                        "pdxu_rakaly",
                        "zip",
                        Settings.getInstance().getAppInstallPath().resolve("rakaly"),
                        false);
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }

            frame.dispose();
        }

        try {
            startApp(args);
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }
    }

    private static void startApp(String[] args) throws IOException {
        List<String> cmdList = new ArrayList<>();
        if (SystemUtils.IS_OS_WINDOWS) {
            cmdList.addAll(List.of(
                    "cmd.exe",
                    "/C",
                    Settings.getInstance().getAppInstallPath()
                            .resolve("app").resolve("bin").resolve("pdxu.bat").toString()));
        } else {
            cmdList.add(Settings.getInstance().getAppInstallPath()
                    .resolve("app").resolve("bin").resolve("pdxu").toString());
        }
        logger.info("Running: " + String.join(" ", cmdList));
        cmdList.addAll(Arrays.asList(args));
        new ProcessBuilder(cmdList).start();
    }


    private static void update(UpdaterGui frame, URL url, String assetName, String fileEnding, Path out, boolean platformSpecific) throws Exception {
        GithubHelper.DownloadInfo info = getInfo(url, assetName, fileEnding, platformSpecific);
        logger.info("Download info: " + info.toString());

        boolean reqUpdate = Settings.getInstance().forceUpdate() || requiresUpdate(info, out);
        if (!reqUpdate) {
            logger.info("No update required");
            return;
        }

        frame.setVisible(true);
        try {
            Path pathToChangelog = downloadFile(info.changelogUrl, p -> {
            }, () -> false);
            String changelog = Files.readString(pathToChangelog);

            JFrame d = new ChangelogGui("Pdx-Unlimiter", changelog);
            d.setVisible(true);

        } catch (Exception e) {
            logger.info("No changelog found");
        }

        logger.info("Downloading " + info.url.toString());
        Path pathToNewest = downloadFile(info.url, frame::setProgress, frame::isDestroyed);
        logger.info("Download complete");
        if (pathToNewest == null) {
            logger.info("Update skipped by user");
            return;
        }
        logger.info("Deleting old version");
        deleteOldVersion(out);
        logger.info("Unzipping new version");
        unzip(pathToNewest, out);
        frame.setVisible(false);
        frame.setProgress(0);
        logger.info("Update completed for " + out.getFileName().toString());
    }

    private static boolean requiresUpdate(GithubHelper.DownloadInfo info, Path p) {
        String v = "";
        try {
            v = Files.readString(p.resolve("version"));
        } catch (IOException e) {
            return true;
        }

        return !v.equals(info.version);
    }
}
