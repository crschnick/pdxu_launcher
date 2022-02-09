package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.lang3.SystemUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

public class Settings {

    private static Settings INSTANCE;
    private Path dataDir;
    private Path logsPath;
    private Path appInstallPath;
    private String version;
    private boolean production;
    private boolean autoupdate;
    private boolean forceUpdate;
    private boolean eu4seEnabled;
    private boolean errorExit;

    public static void init() {
        Properties props = new Properties();
        if (Files.exists(Path.of("pdxu_launcher.properties"))) {
            try {
                var in = Files.newInputStream(Path.of("pdxu_launcher.properties"));
                props.load(in);
                in.close();
            } catch (Exception e) {
                ErrorHandler.handleException(e);
            }
        }

        Settings s = new Settings();

        Path runDir = Path.of(System.getProperty("java.home"));
        Path versionFile = runDir.resolve("version");
        try {
            s.version = Files.exists(versionFile) ? Files.readString(versionFile) : "dev";
        } catch (IOException e) {
            ErrorHandler.handleException(e);
            s.version = "unknown";
        }

        s.production = !s.version.contains("dev");

        s.dataDir = Optional.ofNullable(props.getProperty("dataDir"))
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> {
                    // Legacy support
                    var legacyDataDir = Path.of(System.getProperty("user.home"),
                            SystemUtils.IS_OS_WINDOWS ? "Pdx-Unlimiter" : ".pdx-unlimiter");
                    if (Files.exists(legacyDataDir)) {
                        return legacyDataDir;
                    } else {
                        return getUserDocumentsPath().resolve("Pdx-Unlimiter");
                    }
                });
        s.logsPath = s.dataDir.resolve("logs");

        s.appInstallPath = Optional.ofNullable(props.getProperty("appInstallDir"))
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        return Path.of(System.getenv("LOCALAPPDATA"))
                                .resolve("Programs").resolve("Pdx-Unlimiter");
                    } else {
                        return s.dataDir;
                    }
                });

        s.forceUpdate = Optional.ofNullable(props.getProperty("forceUpdate"))
                .map(Boolean::parseBoolean)
                .orElse(false);

        Path updateFile = s.dataDir.resolve("settings").resolve("update");
        if (Files.exists(updateFile)) {
            try {
                s.autoupdate = Boolean.parseBoolean(Files.readString(updateFile));
            } catch (IOException e) {
                ErrorHandler.handleException(e);
                s.autoupdate = true;
            }
        } else {
            s.autoupdate = true;
        }

        Path errorExitFile = s.dataDir.resolve("settings").resolve("error_exit");
        if (Files.exists(errorExitFile)) {
            try {
                s.errorExit = Boolean.parseBoolean(Files.readString(errorExitFile));
            } catch (IOException e) {
                ErrorHandler.handleException(e);
                s.errorExit = true;
            }
        } else {
            // error_exit is true by default
            s.errorExit = true;
        }

        Path eu4se = s.dataDir.resolve("settings").resolve("eu4saveeditor");
        try {
            s.eu4seEnabled = Files.exists(eu4se) && Boolean.parseBoolean(Files.readString(eu4se));
        } catch (IOException e) {
            ErrorHandler.handleException(e);
            s.eu4seEnabled = false;
        }

        INSTANCE = s;
    }

    private static Path getUserDocumentsPath() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return Path.of(FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
        } else if (SystemUtils.IS_OS_LINUX) {
            return Paths.get(System.getProperty("user.home"), ".local", "share");
        } else if (SystemUtils.IS_OS_MAC) {
            return Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            throw new AssertionError();
        }
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public Path getLogsPath() {
        return logsPath;
    }

    public Path getAppInstallPath() {
        return appInstallPath;
    }

    public Path getAppExecutable() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return appInstallPath.resolve(Path.of("app", "Pdx-Unlimiter.exe"));
        } else if (SystemUtils.IS_OS_LINUX) {
            return appInstallPath.resolve(Path.of("app", "bin", "Pdx-Unlimiter"));
        } else {
            return appInstallPath.resolve(Path.of("app", "Content", "MacOS", "Pdx-Unlimiter"));
        }
    }

    public String getVersion() {
        return version;
    }

    public boolean isProduction() {
        return production;
    }

    public boolean autoupdateEnabled() {
        return autoupdate;
    }

    public boolean forceUpdate() {
        return forceUpdate;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public boolean eu4EditorEnabled() {
        return eu4seEnabled;
    }

    public boolean isErrorExit() {
        return errorExit;
    }
}
