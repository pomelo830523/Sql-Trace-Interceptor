package com.example.g85report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlTraceStep {

    /** Execution order within the wafer (1-based). */
    private int stepOrder;

    /** Logical table group: WAFER_INFO / DIE_RESULT / BIN_DEFINITION */
    private String sqlType;

    /** Complete SQL with real parameter values substituted in. */
    private String sql;

    /** Static human-readable explanation of what this query does. */
    private String explanation;
}
