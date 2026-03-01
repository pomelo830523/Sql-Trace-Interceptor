package com.example.g85report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaferSqlTrace {

    private String waferId;
    private List<SqlTraceStep> steps;
}
