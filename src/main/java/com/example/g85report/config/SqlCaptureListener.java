package com.example.g85report.config;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.NoOpQueryExecutionListener;
import net.ttddyy.dsproxy.proxy.ParameterSetOperation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * datasource-proxy listener that reconstructs the fully-parameterised SQL
 * (replacing '?' with real values) and hands it to SqlCaptureHolder.
 */
public class SqlCaptureListener extends NoOpQueryExecutionListener {

    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (!SqlCaptureHolder.isCapturing()) {
            return;
        }

        for (QueryInfo qi : queryInfoList) {
            String template = qi.getQuery();
            List<List<ParameterSetOperation>> paramsList = qi.getParametersList();

            if (paramsList == null || paramsList.isEmpty()) {
                SqlCaptureHolder.capture(template);
            } else {
                for (List<ParameterSetOperation> params : paramsList) {
                    SqlCaptureHolder.capture(interpolate(template, params));
                }
            }
        }
    }

    /**
     * Replaces '?' placeholders with quoted parameter values, using the
     * parameter index stored in each ParameterSetOperation's args[0].
     */
    private String interpolate(String template, List<ParameterSetOperation> params) {
        // Sort by the JDBC parameter index (1-based integer in args[0])
        List<ParameterSetOperation> sorted = new ArrayList<>(params);
        sorted.sort(Comparator.comparingInt(op -> {
            Object idx = op.getArgs()[0];
            return (idx instanceof Number) ? ((Number) idx).intValue()
                                           : Integer.parseInt(idx.toString());
        }));

        StringBuilder sb = new StringBuilder(template);
        for (ParameterSetOperation op : sorted) {
            int placeholder = sb.indexOf("?");
            if (placeholder == -1) break;
            Object value = op.getArgs().length > 1 ? op.getArgs()[1] : null;
            String replacement = (value == null)
                    ? "NULL"
                    : "'" + value.toString().replace("'", "''") + "'";
            sb.replace(placeholder, placeholder + 1, replacement);
        }
        return sb.toString();
    }
}
