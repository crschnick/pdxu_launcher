package com.crschnick.pdx_unlimiter.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Updater {

    private static Logger logger;

    public static void main(String[] args) {
        Settings.init();
        ErrorHandler.init();

        logger = LoggerFactory.getLogger(Updater.class);
        logger.info("Version " + Settings.getInstance().getVersion());
        logger.info("Passing arguments " + Arrays.toString(args));

        if (InstanceHelper.areOtherLaunchersRunning()) {
            return;
        }

        if (!LauncherUpdater.run()) {
            logger.info("Doing launcher update. Exiting ...");
            return;
        }

        AppUpdater.run(args);
    }
}
