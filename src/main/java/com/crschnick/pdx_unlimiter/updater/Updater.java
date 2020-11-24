package com.crschnick.pdx_unlimiter.updater;

import io.sentry.Sentry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater {
    private static Logger logger;

    private static void exception(Exception e) {
        Sentry.capture(e);
        logger.error("Error occured", e);
    }


    public static void main(String[] args) {
        Path userDataPath = Optional.ofNullable(System.getProperty("pdxu.installDir"))
                .map(Path::of)
                .orElse(Path.of(System.getProperty("user.home"), "Pdx-Unlimiter"));
        Path runDir = Path.of(System.getProperty("java.home"));

        if (isPdxuRunning(userDataPath, runDir)) {
            return;
        }

        Path versionFile = runDir.resolve("version");
        String version = null;
        try {
            version = Files.exists(versionFile) ? Files.readString(versionFile) : "dev";
        } catch (IOException e) {
            version = "unknown";
        }
        boolean prod = !version.equals("dev");
        boolean isBootstrap = version.equals("bootstrap");

        try {
            initErrorHandler(prod, version, runDir);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        UpdaterGui frame = new UpdaterGui();
        if (isBootstrap) {
            runBootstrapper(frame, userDataPath, args);
        } else {
            runUpdater(frame, userDataPath, args);
        }
        frame.dispose();

    }

    private static void runUpdater(UpdaterGui frame, Path installDir, String[] args) {
        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdx_unlimiter/releases/latest/download/"),
                    "pdx_unlimiter",
                    installDir.resolve("app"),
                    true);
        } catch (Exception e) {
            exception(e);
        }


        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdxu_achievements/releases/latest/download/"),
                    "pdxu_achievements",
                    installDir.resolve("achievements"),
                    false);
        } catch (Exception e) {
            exception(e);
        }

        try {
            run(installDir, args);
        } catch (Exception e) {
            exception(e);
        }
    }

    private static void runBootstrapper(UpdaterGui frame, Path installDir, String[] args) {
        try {
            update(frame,
                    new URL("https://github.com/crschnick/pdxu_launcher/releases/latest/download/"),
                    "pdxu_launcher",
                    installDir.resolve("launcher"),
                    true);
        } catch (Exception e) {
            exception(e);
        }

        try {
            runLauncher(installDir, args);
        } catch (IOException e) {
            exception(e);
        }
    }

    private static boolean isPdxuRunning(Path userPath, Path runPath) {
        var app = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .anyMatch(s -> s.equals(userPath.resolve(Path.of("app", "bin", "java.exe")).toString()));

        var launcher = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .anyMatch(s -> s.equals(userPath.resolve(Path.of("launcher", "bin", "java.exe")).toString()));

        var bootstrapper = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .anyMatch(s -> s.equals(runPath.resolve(Path.of( "bin", "java.exe")).toString()));

        return app || launcher || bootstrapper;
    }

    private static void runLauncher(Path dir, String[] args) throws IOException {
        String cmd = dir.resolve("launcher").resolve("bin").resolve("pdxu_launcher.bat").toString();
        logger.info("Running launcher: " + cmd);
        var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
        argList.addAll(Arrays.asList(args));
        new ProcessBuilder(argList).start();
    }

    private static void run(Path dir, String[] args) throws IOException {
        String cmd = dir.resolve("app").resolve("bin").resolve("pdxu.bat").toString();
        logger.info("Running: " + cmd);
        var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
        argList.addAll(Arrays.asList(args));
        new ProcessBuilder(argList).start();
    }

    private static void initErrorHandler(boolean prod, String version, Path p) throws IOException {
        System.setProperty("sentry.dsn", "https://f86d7649617d4c9cb95db5a19811305b@o462618.ingest.sentry.io/5468640");
        System.setProperty("sentry.stacktrace.hidecommon", "false");
        System.setProperty("sentry.stacktrace.app.packages", "");
        System.setProperty("sentry.uncaught.handler.enabled", "true");
        if (prod) {
            FileUtils.forceMkdir(p.resolve("logs").toFile());
            var l = p.resolve("logs").resolve("updater.log");
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            System.setProperty("org.slf4j.simpleLogger.logFile", l.toString());
            System.setProperty("sentry.environment", "production");
            System.setProperty("sentry.version", version);
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("sentry.environment", "dev");
        }
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        logger = LoggerFactory.getLogger(Updater.class);

        logger.info("Initializing updater at " + p.toString() + ", is production: " + prod);
        logger.info("Writing to log file " + p.resolve("logs").resolve("updater.log").toString());
        logger.info("Working directory: " + System.getProperty("user.dir"));
        Sentry.init();
    }

    private static void update(UpdaterGui frame, URL url, String assetName, Path out, boolean platformSpecific) throws Exception {
        Info info = getInfo(url, assetName, platformSpecific);
        logger.info("Download info: " + info.toString());
        if (!requiresUpdate(info, out)) {
            logger.info("No update required");
            return;
        }

        frame.setVisible(true);
        logger.info("Downloading " + info.url.toString());
        Path pathToNewest = downloadNewestVersion(info.url, frame::setProgress);
        logger.info("Download complete");
        logger.info("Deleting old version");
        deleteOldVersion(out);
        logger.info("Unzipping new version");
        unzip(pathToNewest, out);
        frame.setVisible(false);
        logger.info("Update completed for " + out.getFileName().toString());
    }

    private static boolean requiresUpdate(Info info, Path p) {
        Optional<Instant> i = Optional.empty();
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

    public static Path downloadNewestVersion(URL url, Consumer<Float> c) throws Exception {
        byte[] file = executeGet(url, c);
        c.accept(0.0f);
        String tempDir = System.getProperty("java.io.tmpdir");
        Path path = Paths.get(tempDir, url.getFile());
        FileUtils.forceMkdirParent(path.toFile());
        Files.write(path, file);
        return path;
    }

    public static Info getInfo(URL targetURL, String fileName, boolean platformSpecific) throws Exception {
        HttpURLConnection connection = null;

        try {
            //Create connection
            connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "https://github.com/crschnick/pdxu_launcher");
            connection.addRequestProperty("Accept", "*/*");
            connection.setInstanceFollowRedirects(false);

            int responseCode = connection.getResponseCode();
            if (responseCode != 302) {
                throw new IOException("Got http " + responseCode + " for " + targetURL);
            }

            String suffix = platformSpecific ? ("-"
                    + (SystemUtils.IS_OS_WINDOWS ? "windows" : (SystemUtils.IS_OS_LINUX ? "linux" : "mac"))) : "";
            String location = connection.getHeaderField("location");
            String version = Path.of(new URL(location).getFile()).getFileName().toString();
            URL toDownload = new URL(location + "/" + fileName + suffix + ".zip");
            Info i = new Info();
            i.url = toDownload;
            i.version = version;
            return i;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static byte[] executeGet(URL targetURL, Consumer<Float> progress) throws Exception {
        HttpURLConnection connection = null;

        try {
            //Create connection
            connection = (HttpURLConnection) targetURL.openConnection();
            connection.setRequestMethod("GET");
            connection.addRequestProperty("User-Agent", "https://github.com/crschnick/pdxu_launcher");
            connection.addRequestProperty("Accept", "*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Got http " + responseCode + " for " + targetURL);
            }

            InputStream is = connection.getInputStream();
            int size = Integer.parseInt(connection.getHeaderField("Content-Length"));

            byte[] line;
            int bytes = 0;
            ByteBuffer b = ByteBuffer.allocate(size);
            while ((line = is.readNBytes(1000000)).length > 0) {
                b.put(line);
                bytes += line.length;
                if (progress != null) progress.accept((float) bytes / (float) size);
            }
            return b.array();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static class Info {
        public URL url;
        public String version;

        @Override
        public String toString() {
            return "Info{" +
                    "url=" + url +
                    ", version='" + version + '\'' +
                    '}';
        }
    }
}
