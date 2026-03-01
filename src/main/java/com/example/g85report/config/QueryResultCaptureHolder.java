package com.example.g85report.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ThreadLocal holder for query result sets, parallel to SqlCaptureHolder.
 * Each captured entry corresponds to one repository List<?> return, in execution order.
 */
public class QueryResultCaptureHolder {

    private static final ThreadLocal<List<List<Map<String, Object>>>> holder = new ThreadLocal<>();

    public static void startCapture() {
        holder.set(new ArrayList<>());
    }

    public static void stopCapture() {
        holder.remove();
    }

    public static boolean isCapturing() {
        return holder.get() != null;
    }

    public static void capture(List<Map<String, Object>> rows) {
        List<List<Map<String, Object>>> list = holder.get();
        if (list != null) {
            list.add(rows);
        }
    }

    public static List<List<Map<String, Object>>> getCaptured() {
        List<List<Map<String, Object>>> list = holder.get();
        return list != null ? List.copyOf(list) : List.of();
    }
}
