package com.nayan.scheduler.core.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

public class Logger {
    private static String LOG_FILE = "scheduler.log";

    public static void initialize() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, false))) {
            // Opening with append=false clears the file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized void log(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(LOG_FILE, true))) {
            writer.println("[" + Instant.now() + "] " + message);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

}
