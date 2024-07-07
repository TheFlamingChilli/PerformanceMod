package com.theflamingchilli.performancemod.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerfTimer {

    private static final Logger LOGGER = LoggerFactory.getLogger("PerformanceMod-PERFTIMER");
    public static long start() {
        return System.nanoTime();
    }

    public static long begin() {
        return System.nanoTime();
    }

    public static void end(long startTime, String message) {
        long end = System.nanoTime();
        long nanoseconds = end - startTime;
        long microseconds = nanoseconds / 1000;
        long milliseconds = microseconds / 1000;
        LOGGER.info(message + " [" + milliseconds + "ms - " + microseconds + "µs - " + nanoseconds + "ns]");
    }
}
