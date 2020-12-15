package com.crschnick.pdx_unlimiter.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;

public class InstanceHandler {

    private static Logger logger = LoggerFactory.getLogger(InstanceHandler.class);

    public static boolean checkForOtherPdxuInstances() {
        var launcher = ProcessHandle.allProcesses()
                .map(h -> h.info().command().orElse(""))
                .filter(s -> s.startsWith(Settings.getInstance().getInstallPath().resolve(
                        Path.of("launcher", "bin", "java.exe")).toString()))
                .collect(Collectors.toList());
        launcher.forEach(s -> logger.info("Detected running launcher: " + s));

        if (Settings.getInstance().isBootstrap()) {
            Path runPath = Path.of(System.getProperty("java.home"));
            var bootsrapExecutable = runPath.getParent().resolve("Pdx-Unlimiter.exe");
            var bootstrappers = ProcessHandle.allProcesses()
                    .map(h -> h.info().command().orElse(""))
                    .filter(s -> s.startsWith(bootsrapExecutable.toString()))
                    .collect(Collectors.toList());
            bootstrappers.forEach(s -> logger.info("Detected running bootstrapper: " + s));
            int count = Settings.getInstance().isProduction() ? bootstrappers.size() : bootstrappers.size() + 1;

            // Starting launcher initially and no other bootstrapper instance is running.
            return launcher.size() == 0 && count == 1;
        } else {
            var app = ProcessHandle.allProcesses()
                    .map(h -> h.info().command().orElse(""))
                    .filter(s -> s.startsWith(Settings.getInstance().getInstallPath().resolve(
                            Path.of("app", "bin", "java.exe")).toString()))
                    .collect(Collectors.toList());
            app.forEach(s -> logger.info("Detected running app: " + s));

            if (app.size() > 0) {
                JOptionPane.showMessageDialog(null, "thank you for using java");
            }

            // Starting app initially and no other launcher instance is running.
            return app.size() == 0 && launcher.size() == 1;
        }
    }
}
