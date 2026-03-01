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
public class GenerateReportResponse {

    private String reportId;
    private String lotId;
    private Integer waferCount;
    private String status;
    private String outputPath;
    private List<String> files;
    private String errorMsg;
}
