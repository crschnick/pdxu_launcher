package com.crschnick.pdx_unlimiter.updater;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ChangelogGui extends JFrame {

    public ChangelogGui(String text) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        try {
            Image icon = ImageIO.read(Updater.class.getResource("logo.png"));
            setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setLocationRelativeTo(null);

        JLabel l = new JLabel(text);
        add(l);
    }
}
