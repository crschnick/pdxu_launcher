package com.crschnick.pdx_unlimiter.updater;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.IOException;

public class UpdaterGui extends JFrame {

    private Image image;

    private float progress;
    private boolean destroyed;

    public UpdaterGui() {
        setTitle("Pdx-Unlimiter Updater");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                UpdaterGui.this.destroyed = true;
                e.getWindow().dispose();
            }
        });
        try {
            Image icon = ImageIO.read(Updater.class.getResource("logo.png"));
            setIconImage(icon);
            image = ImageIO.read(Updater.class.getResource("splash.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSize(image.getWidth(this), image.getHeight(this));
        setLocationRelativeTo(null);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setProgress(float progress) {
        this.progress = progress;
        repaint();
    }

    private void renderSplashFrame(Graphics g) {
        g.drawImage(image, 0, 0, this);
        g.setColor(Color.WHITE);
        g.fillRect(50, 150, (int) (520 * progress), 40);
    }

    public void paint(Graphics g) {
        renderSplashFrame(g);
    }
}
