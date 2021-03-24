package com.crschnick.pdx_unlimiter.updater.gui;

import com.crschnick.pdx_unlimiter.updater.Updater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.crschnick.pdx_unlimiter.updater.util.GithubHelper.downloadFile;

public class ChangelogGui extends JFrame {

    private static final Logger logger = LoggerFactory.getLogger(ChangelogGui.class);

    public ChangelogGui(String name, String text) {
        JTextArea l = new JTextArea(text);
        l.setEditable(false);
        l.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        add(l);

        setTitle(name + " changelog");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        try {
            Image icon = ImageIO.read(Updater.class.getResource("logo.png"));
            setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setMinimumSize(new Dimension(380, 160));
        setSize(l.getPreferredSize().width + 30, l.getPreferredSize().height + 80);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
    }

    public static void displayChangelog(String name, URL url) {
        try {
            Path pathToChangelog = downloadFile(url, p -> {
            }, () -> false);
            if (pathToChangelog == null) {
                return;
            }

            String changelog = Files.readString(pathToChangelog);

            JFrame d = new ChangelogGui(name, changelog);
            d.setVisible(true);
            Instant start = Instant.now();
            new Thread(() -> {
                while (true) {
                    // Exit if changelog is closed manually
                    if (!d.isVisible()) {
                        return;
                    }

                    // Close changelog after 20s
                    if (Duration.between(start, Instant.now())
                            .compareTo(Duration.of(30, ChronoUnit.SECONDS)) > 0) {
                        d.dispose();
                        return;
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                    }
                }
            }).start();
        } catch (Exception e) {
            logger.info("No changelog found");
        }
    }
}
