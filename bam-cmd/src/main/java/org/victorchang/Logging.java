package org.victorchang;

import java.util.logging.Level;

public class Logging {

    public static void configure(boolean verbose) {
        Level level = verbose ? Level.ALL : Level.SEVERE;
        java.util.logging.Logger.getLogger("").setLevel(level);

        // More friendly single-line logging format
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "[%1$tF %1$tT] [%4$-7s] %5$s %n");
    }
}
