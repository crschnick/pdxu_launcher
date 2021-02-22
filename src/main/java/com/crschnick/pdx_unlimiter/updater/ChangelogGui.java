package com.crschnick.pdx_unlimiter.updater;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ChangelogGui extends JFrame {

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
        setSize(l.getPreferredSize());
        setLocationRelativeTo(null);
    }
}
