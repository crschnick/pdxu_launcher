package com.crschnick.pdx_unlimiter.updater;

import com.crschnick.pdx_unlimiter.updater.util.GithubHelper;
import com.crschnick.pdx_unlimiter.updater.util.InstanceHelper;
import com.crschnick.pdx_unlimiter.updater.util.UpdateHelper;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.getInfo;

public class AppUpdater {

    private static final Logger logger = LoggerFactory.getLogger(AppUpdater.class);

    public static void run(String[] args) {
        boolean doUpdate = InstanceHelper.shouldUpdateApp(args);
        logger.info("Doing app update: " + doUpdate);
        if (doUpdate) {
            try {
                updateApp();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }

            try {
                updateRakaly();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
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

    private static void updateApp() throws Exception {
        var out = Settings.getInstance().getAppInstallPath().resolve("app");
        var url = new URL("https://github.com/crschnick/pdx_unlimiter/releases/latest/download/");

        GithubHelper.DownloadInfo info = getInfo(url, "pdx_unlimiter", "zip", true);
        logger.info("Download info: " + info.toString());
        boolean reqUpdate = Settings.getInstance().forceUpdate() || requiresUpdate(info, out);
        if (!reqUpdate) {
            logger.info("No update required");
            return;
        }

        // Write latest version
        Files.writeString(Settings.getInstance().getDataDir().resolve("settings").resolve("latest"), info.version);
        UpdateHelper.update("Pdx-Unlimiter", out, info);
    }

    private static void updateRakaly() throws Exception {
        var out = Settings.getInstance().getAppInstallPath().resolve("rakaly");
        var url = new URL("https://github.com/crschnick/pdxu_rakaly/releases/latest/download/");

        GithubHelper.DownloadInfo info = getInfo(url, "pdxu_rakaly", "zip", false);
        logger.info("Download info: " + info.toString());
        boolean reqUpdate = Settings.getInstance().forceUpdate() || requiresUpdate(info, out);
        if (!reqUpdate) {
            logger.info("No update required");
            return;
        }

        UpdateHelper.update("Rakaly", out, info);
    }

    private static boolean requiresUpdate(GithubHelper.DownloadInfo info, Path p) {
        String v;
        try {
            v = Files.readString(p.resolve("version"));
        } catch (IOException e) {
            return true;
        }

        return !v.equals(info.version);
    }
}
