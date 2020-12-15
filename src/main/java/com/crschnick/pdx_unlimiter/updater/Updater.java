package com.crschnick.pdx_unlimiter.updater;

import io.sentry.Sentry;
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

public class Updater {

    private static Logger logger;

    private static void exception(Exception e) {
        Sentry.capture(e);
        logger.error("Error occured", e);
    }

    public static void main(String[] args) {
        Settings.init();
        initErrorHandler();

        logger.info("Version " + Settings.getInstance().getVersion());
        logger.info("Passing arguments " + Arrays.toString(args));

        boolean doUpdate = Settings.getInstance().doUpdate() && InstanceHandler.checkForOtherPdxuInstances();
        logger.info("Doing update: " + doUpdate);
        if (doUpdate) {
            UpdaterGui frame = new UpdaterGui();
            try {
                if (Settings.getInstance().isBootstrap()) {
                    runBootstrapper(frame, args);
                } else {
                    runUpdater(frame, args);
                }
            } catch (Exception e) {
                exception(e);
            }
            frame.dispose();
        }

        try {
            if (Settings.getInstance().isBootstrap()) {
                startLauncher(args);
            } else {
                startApp(args);
            }
        } catch (Exception e) {
            exception(e);
        }
    }

    private static void runUpdater(UpdaterGui frame, String[] args) {
        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdx_unlimiter/releases/latest/download/"),
                    "pdx_unlimiter",
                    Settings.getInstance().getInstallPath().resolve("app"),
                    true);
        } catch (Exception e) {
            exception(e);
        }


        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdxu_achievements/releases/latest/download/"),
                    "pdxu_achievements",
                    Settings.getInstance().getInstallPath().resolve("achievements"),
                    false);
        } catch (Exception e) {
            exception(e);
        }
    }

    private static void runBootstrapper(UpdaterGui frame, String[] args) {
        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdxu_launcher/releases/latest/download/"),
                    "pdxu_launcher",
                    Settings.getInstance().getInstallPath().resolve("launcher"),
                    true);
        } catch (Exception e) {
            exception(e);
        }

        try {
            startLauncher(args);
        } catch (IOException e) {
            exception(e);
        }
    }

    private static void startLauncher(String[] args) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            String cmd = Settings.getInstance().getInstallPath()
                    .resolve("launcher").resolve("bin").resolve("pdxu_launcher.bat").toString();
            logger.info("Running launcher: " + cmd);
            var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
            argList.addAll(Arrays.asList(args));
            new ProcessBuilder(argList).start();
        } else {
            new ProcessBuilder(Settings.getInstance().getInstallPath()
                    .resolve("launcher").resolve("bin").resolve("pdxu_launcher").toString()).start();
        }
    }

    private static void startApp(String[] args) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            String cmd = Settings.getInstance().getInstallPath()
                    .resolve("app").resolve("bin").resolve("pdxu.bat").toString();
            logger.info("Running: " + cmd);
            var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
            argList.addAll(Arrays.asList(args));
            new ProcessBuilder(argList).start();
        } else {
            new ProcessBuilder(Settings.getInstance().getInstallPath()
                    .resolve("launcher").resolve("bin").resolve("pdxu").toString()).start();
        }
    }

    private static void initErrorHandler() {
        System.setProperty("sentry.dsn", "https://f86d7649617d4c9cb95db5a19811305b@o462618.ingest.sentry.io/5468640");
        System.setProperty("sentry.stacktrace.hidecommon", "false");
        System.setProperty("sentry.stacktrace.app.packages", "");
        System.setProperty("sentry.uncaught.handler.enabled", "true");
        System.setProperty("sentry.servername", "");
        if (Settings.getInstance().isProduction()) {
            try {
                FileUtils.forceMkdir(Settings.getInstance().getLogsPath().toFile());
                var l = Settings.getInstance().getLogsPath().resolve(Settings.getInstance().isBootstrap() ?
                        "bootstrapper.log" : "launcher.log");
                System.setProperty("org.slf4j.simpleLogger.logFile", l.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            System.setProperty("sentry.environment", "production");
            System.setProperty("sentry.release", Settings.getInstance().getVersion());
        } else {
            //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("sentry.environment", "dev");
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        logger = LoggerFactory.getLogger(Updater.class);

        logger.info("Initializing with " + "production: " + Settings.getInstance().isProduction() +
                ", bootstrap: " + Settings.getInstance().isBootstrap());
        Sentry.init();
    }

    private static void update(UpdaterGui frame, URL url, String assetName, Path out, boolean platformSpecific) throws Exception {
        GithubHelper.DownloadInfo info = getInfo(url, assetName, platformSpecific);
        logger.info("Download info: " + info.toString());
        if (!Settings.getInstance().forceUpdate() && !requiresUpdate(info, out)) {
            logger.info("No update required");
            return;
        }

        try {
            Path pathToChangelog = downloadFile(info.changelogUrl, p -> {
            });
            String changelog = Files.readString(pathToChangelog);

            JDialog d = new JDialog(new ChangelogGui(changelog), "dialog Box");
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
