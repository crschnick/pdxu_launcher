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

        try {
            updateApp(doUpdate);
        } catch (Exception e) {
            ErrorHandler.handleException(e);
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
                    Settings.getInstance().getAppInstallPath()
                            .resolve("app").resolve("Pdx-Unlimiter.exe").toString()));
        } else if (SystemUtils.IS_OS_LINUX) {
            cmdList.add(Settings.getInstance().getAppInstallPath()
                    .resolve("app").resolve("bin").resolve("pdxu").toString());
        } else if (SystemUtils.IS_OS_MAC) {
            cmdList.add(Settings.getInstance().getAppInstallPath()
                    .resolve("Contents").resolve("MacOS").resolve("Pdx-Unlimiter").toString());
        }
        cmdList.addAll(Arrays.asList(args));
        logger.info("Running: " + String.join(" ", cmdList));
        new ProcessBuilder(cmdList).start();
    }

    private static void updateApp(boolean doUpdate) throws Exception {
        var out = Settings.getInstance().getAppInstallPath().resolve("app");
        var url = new URL("https://github.com/crschnick/pdx_unlimiter/releases/latest/download/");

        var infoOpt = getInfo(url, "pdx_unlimiter", "zip", true);
        if (infoOpt.isEmpty()) {
            return;
        }
        var info = infoOpt.get();

        logger.info("Download info: " + info.toString());

        // Write latest version
        var latestFile = Settings.getInstance().getDataDir().resolve("settings").resolve("latest");
        Files.createDirectories(latestFile.getParent());
        Files.writeString(latestFile, info.version);

        boolean reqUpdate = Settings.getInstance().forceUpdate() || requiresUpdate(info, out);
        if (!reqUpdate) {
            logger.info("No update required");
            return;
        }

        if (doUpdate) {
            UpdateHelper.update("Pdx-Unlimiter", out, info);
        }
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
