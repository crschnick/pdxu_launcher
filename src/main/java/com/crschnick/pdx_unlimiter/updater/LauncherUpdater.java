package com.crschnick.pdx_unlimiter.updater;

import com.crschnick.pdx_unlimiter.updater.gui.UpdaterGui;
import com.crschnick.pdx_unlimiter.updater.util.GithubHelper;
import com.crschnick.pdx_unlimiter.updater.util.InstanceHelper;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.downloadFile;
import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.getInfo;

public class LauncherUpdater {

    private static final Logger logger = LoggerFactory.getLogger(LauncherUpdater.class);

    private static boolean showUpdateDialog(String changelog) {
        Icon icon = null;
        try {
            icon = new ImageIcon(ImageIO.read(ErrorHandler.class.getResource("logo.png"))
                    .getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {
        }

        int r = JOptionPane.showConfirmDialog(null, """
                        A Pdx-Unlimiter launcher update is available:
                        """ + (changelog != null ? changelog : "") +
                        """
                                                        
                                                        
                                Do you want to install it?         
                                Afterwards, you have to start the Pdx-Unlimiter again.""",
                "Pdx-Unlimiter", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, icon);
        return r == JOptionPane.YES_OPTION;
    }

    public static boolean update(String[] args) {
        boolean doUpdate = InstanceHelper.shouldUpdateLauncher(args);
        logger.info("Doing launcher update: " + doUpdate);
        if (!doUpdate) {
            return false;
        }

        GithubHelper.DownloadInfo info;
        try {
            info = getInfo(
                    new URL("https://github.com/crschnick/pdxu_launcher/releases/latest/download/"),
                    "pdxu_installer",
                    SystemUtils.IS_OS_WINDOWS ? "msi" : "deb",
                    true);

        } catch (Exception e) {
            ErrorHandler.handleException(e);
            return false;
        }

        logger.info("Download info: " + info.toString());

        String changelog = null;
        try {
            Path pathToChangelog = downloadFile(info.changelogUrl, p -> {
            }, () -> false);
            if (pathToChangelog != null) {
                changelog = Files.readString(pathToChangelog);
            }
        } catch (Exception e) {
            logger.info("No changelog found");
        }

        boolean reqUpdate = (Settings.getInstance().forceUpdate() ||
                !info.version.equals(Settings.getInstance().getVersion())) && showUpdateDialog(changelog);
        if (!reqUpdate) {
            logger.info("No launcher update required");
            return false;
        }

        UpdaterGui frame = new UpdaterGui("Pdx-Unlimiter Launcher");
        frame.setVisible(true);
        logger.info("Downloading " + info.url.toString());
        try {
            Path pathToNewest = Settings.getInstance().getLauncherInstaller().orElse(
                    downloadFile(info.url, frame::setProgress, frame::isDestroyed));
            frame.dispose();
            if (pathToNewest == null) {
                return false;
            }

            if (SystemUtils.IS_OS_WINDOWS) {
                var l = Settings.getInstance().getLauncherInstallPath();
                logger.info("Existing launcher install at: " + l.map(Path::toString).orElse("none"));

                var cmd = new ArrayList<>(java.util.List.of(
                        "msiexec",
                        "/i", pathToNewest.toString(),
                        "/li", Settings.getInstance().getLogsPath().resolve("installer_" + info.version + ".log").toString(),
                        "/qb+"
                ));
                l.ifPresent(p -> cmd.add("INSTALLDIR=\"" + p.toString() + "\""));

                var toRun = String.join(" ", cmd);
                logger.debug("Running " + toRun);

                // Use Runtime since ProcessBuilder somehow breaks when using = signs
                Runtime.getRuntime().exec(toRun);
            } else {
                var pw = new ProcessBuilder(
                        "/usr/bin/ssh-askpass",
                        "A Pdx-Unlimiter launcher update is available. " +
                                "To start it, your sudo password is required.")
                        .start();
                String pwString = new String(pw.getInputStream().readAllBytes()).replace("\n", "");

                var proc = new ProcessBuilder(
                        "/bin/sh",
                        "-c",
                        "echo " + pwString + " | sudo -S apt install --reinstall " + pathToNewest.toString());
                proc.redirectErrorStream(true);
                proc.redirectOutput(Settings.getInstance().getLogsPath().resolve("installer_" + info.version + ".log").toFile());
                proc.start();
            }
            return true;
        } catch (Exception e) {
            ErrorHandler.handleException(e);
        }
        frame.dispose();
        return false;
    }
}
