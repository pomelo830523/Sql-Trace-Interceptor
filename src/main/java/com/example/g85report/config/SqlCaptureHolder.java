package com.example.g85report.config;

import java.util.ArrayList;
import java.util.List;

/**
 * ThreadLocal-based holder for capturing SQL statements during a request scope.
 * SQL is never persisted; it lives only for the duration of the current thread's capture session.
 */
public class SqlCaptureHolder {

    private static final ThreadLocal<List<String>> holder = new ThreadLocal<>();

    public static void startCapture() {
        holder.set(new ArrayList<>());
    }

    public static void stopCapture() {
        holder.remove();
    }

    public static boolean isCapturing() {
        return holder.get() != null;
    }

    public static void capture(String sql) {
        List<String> list = holder.get();
        if (list != null) {
            list.add(sql);
        }
    }

    /** Returns the current list without clearing it. */
    public static List<String> getCaptured() {
        List<String> list = holder.get();
        return list != null ? List.copyOf(list) : List.of();
    }
}
