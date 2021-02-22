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
    private Path launcherInstallPath;
    private Path launcherInstaller;
    private String version;
    private boolean production;
    private boolean updateLauncher;
    private boolean autoupdate;
    private boolean forceUpdate;
    private boolean eu4seEnabled;

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

        s.launcherInstaller = Path.of(props.getProperty("launcherInstaller"));

        s.launcherInstallPath = s.production ?
                Path.of(System.getProperty("java.home")).getParent() :
                Path.of(props.getProperty("launcherInstallLocation"));

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

        s.updateLauncher = Optional.ofNullable(props.getProperty("updateLauncher"))
                .map(Boolean::parseBoolean)
                .orElse(true);

        Path updateFile = s.dataDir.resolve("settings").resolve("update");
        if (Files.exists(updateFile)) {
            try {
                s.autoupdate = Boolean.parseBoolean(Files.readString(updateFile));
            } catch (IOException e) {
                e.printStackTrace();
                s.autoupdate = true;
            }
        } else {
            s.autoupdate = true;
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
        } else {
            return Paths.get(System.getProperty("user.home"), ".local", "share");
        }
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public Path getLogsPath() {
        return logsPath;
    }

    public Optional<Path> getLauncherInstallPath() {
        return Optional.ofNullable(launcherInstallPath);
    }

    public Path getAppInstallPath() {
        return appInstallPath;
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

    public boolean updateLauncher() {
        return updateLauncher;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public boolean eu4EditorEnabled() {
        return eu4seEnabled;
    }

    public Optional<Path> getLauncherInstaller() {
        return Optional.ofNullable(launcherInstaller);
    }
}
