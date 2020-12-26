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

import static com.crschnick.pdx_unlimiter.updater.GithubHelper.downloadFile;
import static com.crschnick.pdx_unlimiter.updater.GithubHelper.getInfo;

public class AppUpdater {

    private static Logger logger = LoggerFactory.getLogger(AppUpdater.class);

    public static void run(String[] args) {
        boolean doUpdate = Settings.getInstance().autoupdateEnabled() &&
                (args.length == 0 || InstanceHelper.checkForOtherPdxuInstances());
        logger.info("Doing app update: " + doUpdate);
        if (!doUpdate) {
            return;
        }

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

        try {
            startApp(args);
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }
    }

    private static void startApp(String[] args) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            String cmd = Settings.getInstance().getAppInstallPath()
                    .resolve("app").resolve("bin").resolve("pdxu.bat").toString();
            logger.info("Running: " + cmd);
            var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
            argList.addAll(Arrays.asList(args));
            var proc = new ProcessBuilder(argList).start();
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
        }
    }



    private static void update(UpdaterGui frame, URL url, String assetName, String fileEnding, Path out, boolean platformSpecific) throws Exception {
        GithubHelper.DownloadInfo info = getInfo(url, assetName, fileEnding, platformSpecific);
        logger.info("Download info: " + info.toString());

        boolean reqUpdate = Settings.getInstance().forceUpdate() || requiresUpdate(info, out);
        if (!reqUpdate) {
            logger.info("No update required");
            return;
        }

        try {
            Path pathToChangelog = downloadFile(info.changelogUrl, p -> {
            });
            String changelog = Files.readString(pathToChangelog);

            JDialog d = new JDialog(new ChangelogGui(changelog), "Changelog");
            d.setVisible(true);

        } catch (Exception e) {
            logger.info("No changelog found");
        }

        frame.setVisible(true);
        logger.info("Downloading " + info.url.toString());
        Path pathToNewest = downloadFile(info.url, frame::setProgress);
        logger.info("Download complete");
        logger.info("Deleting old version");
        deleteOldVersion(out);
        logger.info("Unzipping new version");
        unzip(pathToNewest, out);
        frame.setVisible(false);
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

    public static void deleteOldVersion(Path path) throws Exception {
        File f = path.toFile();
        FileUtils.deleteDirectory(f);
        FileUtils.forceMkdir(f);
    }

    private static void unzip(Path zipFilePath, Path destDir) throws Exception {
        File dir = destDir.toFile();
        if (dir.exists()) {
            FileUtils.deleteDirectory(dir);
        }
        FileUtils.forceMkdir(dir);

        ZipFile f = new ZipFile(zipFilePath.toString());
        for (Iterator<? extends ZipEntry> it = f.stream().iterator(); it.hasNext(); ) {
            ZipEntry e = it.next();
            String fileName = e.getName();
            Path p = destDir.resolve(fileName);
            if (e.isDirectory()) {
                FileUtils.forceMkdir(p.toFile());
            } else {
                Files.write(p, f.getInputStream(e).readAllBytes());
            }
        }
        f.close();
    }
}
