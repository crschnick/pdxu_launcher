package com.crschnick.pdx_unlimiter.updater;

import org.apache.commons.lang3.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class Settings {

    private static Settings INSTANCE;
    private Path logsPath;
    private Path installPath;
    private String version;
    private boolean production;
    private boolean doUpdate;
    private boolean forceUpdate;
    private boolean bootstrap;

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

        Path dataDir = Optional.ofNullable(props.getProperty("dataDir"))
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElse(Path.of(System.getProperty("user.home"), "Pdx-Unlimiter"));
        s.logsPath = SystemUtils.IS_OS_WINDOWS ?
                dataDir.resolve("logs") : Path.of("var", "logs", "Pdx-Unlimiter");

        s.installPath = Optional.ofNullable(props.getProperty("installDir"))
                .map(Path::of)
                .filter(Path::isAbsolute)
                .orElseGet(() -> {
                    if (SystemUtils.IS_OS_WINDOWS) {
                        return Path.of(System.getenv("LOCALAPPDATA"))
                                .resolve("Programs").resolve("Pdx-Unlimiter");
                    } else {
                        return Path.of(System.getProperty("user.home"), ".Pdx-Unlimiter");
                    }
                });

        s.forceUpdate = Optional.ofNullable(props.getProperty("forceUpdate"))
                .map(Boolean::parseBoolean)
                .orElse(false);

        Path updateFile = dataDir.resolve("settings").resolve("update");
        if (Files.exists(updateFile)) {
            try {
                s.doUpdate = Boolean.parseBoolean(Files.readString(updateFile));
            } catch (IOException e) {
                e.printStackTrace();
                s.doUpdate = true;
            }
        } else {
            s.doUpdate = true;
        }

        Path runDir = Path.of(System.getProperty("java.home"));
        Path versionFile = runDir.resolve("version");
        try {
            s.version = Files.exists(versionFile) ? Files.readString(versionFile) : "dev";
        } catch (IOException e) {
            ErrorHandler.handleException(e);
        }

        s.production = !s.version.contains("dev");

        s.bootstrap = Optional.ofNullable(props.getProperty("bootstrap"))
                .map(Boolean::parseBoolean)
                .orElse(s.version.contains("bootstrap"));

        INSTANCE = s;
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public Path getLogsPath() {
        return logsPath;
    }

    public Path getInstallPath() {
        return installPath;
    }

    public String getVersion() {
        return version;
    }

    public boolean isProduction() {
        return production;
    }

    public boolean autoupdateEnabled() {
        return doUpdate;
    }

    public boolean forceUpdate() {
        return forceUpdate;
    }

    public boolean isBootstrap() {
        return bootstrap;
    }
}
