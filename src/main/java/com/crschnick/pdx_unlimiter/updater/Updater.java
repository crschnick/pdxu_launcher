package com.crschnick.pdx_unlimiter.updater;

import com.crschnick.pdx_unlimiter.updater.util.InstanceHelper;
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

        Eu4SeUpdater.update();
        AppUpdater.run(args);
    }
}
