package com.crschnick.pdx_unlimiter.updater;

import io.sentry.Sentry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Updater {

    private static Logger logger;

    private static void exception(Exception e) {
        Sentry.capture(e);
        logger.error("Error occured", e);
    }

    public static void main(String[] args) {
        Path logsPath = Optional.ofNullable(System.getProperty("pdxu.userDir"))
                .map(Path::of)
                .map(p -> p.resolve("logs"))
                .orElseGet(() -> {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        return Path.of(System.getProperty("user.home"), "Pdx-Unlimiter", "logs");
                    } else {
                        return Path.of("var", "logs", "Pdx-Unlimiter");
                    }
                });

        Path installPath = Optional.ofNullable(System.getProperty("pdxu.installDir"))
                .map(Path::of)
                .orElseGet(() -> {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        return Path.of(System.getenv("LOCALAPPDATA"))
                                .resolve("Programs").resolve("Pdx-Unlimiter");
                    } else {
                        return Path.of(System.getProperty("user.home"), ".Pdx-Unlimiter");
                    }
                });

        Path runDir = Path.of(System.getProperty("java.home"));
        Path versionFile = runDir.resolve("version");
        String version;
        try {
            version = Files.exists(versionFile) ? Files.readString(versionFile) : "dev";
        } catch (IOException e) {
            version = "unknown";
        }
        boolean prod = !version.equals("dev");
        boolean isBootstrap = version.contains("bootstrap");

        try {
            initErrorHandler(isBootstrap, prod, version, logsPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        logger.info("Passing arguments " + Arrays.toString(args));

        boolean doUpdate = shouldDoUpdate(installPath, runDir, isBootstrap);
        logger.info("Doing update: " + doUpdate);

        UpdaterGui frame = new UpdaterGui();
        if (isBootstrap) {
            runBootstrapper(frame, installPath, doUpdate, args);
        } else {
            runUpdater(frame, installPath, doUpdate, args);
        }
        frame.dispose();

    }

    private static void runUpdater(UpdaterGui frame, Path installDir, boolean doUpdate, String[] args) {
        if (doUpdate) {
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
        }

        try {
            run(installDir, args);
        } catch (Exception e) {
            exception(e);
        }
    }

    private static void runBootstrapper(UpdaterGui frame, Path installDir, boolean doUpdate, String[] args) {
        if (doUpdate) {
            try {
                update(frame,
                        new URL("https://github.com/crschnick/pdxu_launcher/releases/latest/download/"),
                        "pdxu_launcher",
                        installDir.resolve("launcher"),
                        true);
            } catch (Exception e) {
                exception(e);
            }
        }

        try {
            runLauncher(installDir, args);
        } catch (IOException e) {
            exception(e);
        }
    }

    private static boolean shouldDoUpdate(Path installPath, Path runPath, boolean isBootstrapper) {
        var app = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.equals(installPath.resolve(Path.of("app", "bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        app.forEach(s -> logger.debug("Detected running app: " + s));

        var launcher = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.equals(installPath.resolve(Path.of("launcher", "bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        launcher.forEach(s -> logger.debug("Detected running launcher: " + s));

        var bootstrappers = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.equals(runPath.resolve(Path.of("bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        bootstrappers.forEach(s -> logger.debug("Detected running bootstrapper: " + s));

        if (isBootstrapper) {
            // Starting launcher initially and no other bootstrapper instance is running.
            return launcher.size() == 0 && bootstrappers.size() == 1;
        } else {
            // Starting app initially and no other launcher instance is running.
            return app.size() == 0 && launcher.size() == 1;
        }
    }

    private static void runLauncher(Path dir, String[] args) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            String cmd = dir.resolve("launcher").resolve("bin").resolve("pdxu_launcher.bat").toString();
            logger.info("Running launcher: " + cmd);
            var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
            argList.addAll(Arrays.asList(args));
            new ProcessBuilder(argList).start();
        } else {
            new ProcessBuilder(dir.resolve("launcher").resolve("bin").resolve("pdxu_launcher").toString()).start();
        }
    }

    private static void run(Path dir, String[] args) throws IOException {
        if (SystemUtils.IS_OS_WINDOWS) {
            String cmd = dir.resolve("app").resolve("bin").resolve("pdxu.bat").toString();
            logger.info("Running: " + cmd);
            var argList = new ArrayList<>(List.of("cmd.exe", "/C", cmd));
            argList.addAll(Arrays.asList(args));
            new ProcessBuilder(argList).start();
        } else {
            new ProcessBuilder(dir.resolve("launcher").resolve("bin").resolve("pdxu").toString()).start();
        }
    }

    private static void initErrorHandler(boolean bootstrapper, boolean prod, String version, Path logDir) throws IOException {
        System.setProperty("sentry.dsn", "https://f86d7649617d4c9cb95db5a19811305b@o462618.ingest.sentry.io/5468640");
        System.setProperty("sentry.stacktrace.hidecommon", "false");
        System.setProperty("sentry.stacktrace.app.packages", "");
        System.setProperty("sentry.uncaught.handler.enabled", "true");
        System.setProperty("sentry.servername", "");
        if (prod) {
            FileUtils.forceMkdir(logDir.toFile());
            var l = logDir.resolve(bootstrapper ? "bootstrapper.log" : "launcher.log");
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
            System.setProperty("org.slf4j.simpleLogger.logFile", l.toString());
            System.setProperty("sentry.environment", "production");
            System.setProperty("sentry.release", version);
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
            System.setProperty("sentry.environment", "dev");
        }
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");

        logger = LoggerFactory.getLogger(Updater.class);

        logger.info("Initializing with " + "production: " + prod + ", bootstrap: " + bootstrapper);
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
