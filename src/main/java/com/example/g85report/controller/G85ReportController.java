package com.example.g85report.controller;

import com.example.g85report.config.SqlCaptureHolder;
import com.example.g85report.dto.GenerateReportRequest;
import com.example.g85report.dto.GenerateReportResponse;
import com.example.g85report.entity.ReportLog;
import com.example.g85report.service.G85ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api/report/g85")
@RequiredArgsConstructor
public class G85ReportController {

    private final G85ReportService reportService;

    /**
     * 產生整個 Lot 的 G85 XML 報表
     * POST /api/report/g85/generate
     * Body: { "lotId": "999999" }
     */
    @PostMapping("/generate")
    public ResponseEntity<GenerateReportResponse> generate(@RequestBody GenerateReportRequest request) {
        GenerateReportResponse response = reportService.generate(request);

        if (SqlCaptureHolder.isCapturing()) {
            response.setSqlTrace(
                reportService.buildTrace(SqlCaptureHolder.getCaptured())
            );
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 下載指定 Wafer 的 XML 檔案
     * GET /api/report/g85/download/{waferId}
     */
    @GetMapping("/download/{waferId}")
    public ResponseEntity<Resource> download(@PathVariable String waferId) {
        File xmlFile = findXmlFile(new File("./reports"), waferId + ".xml");

        if (xmlFile == null || !xmlFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(xmlFile);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + waferId + ".xml\"")
                .body(resource);
    }

    /**
     * 查詢歷史報表紀錄
     * GET /api/report/g85/history?lotId=999999
     */
    @GetMapping("/history")
    public ResponseEntity<List<ReportLog>> history(@RequestParam String lotId) {
        return ResponseEntity.ok(reportService.getHistory(lotId));
    }

    /** 遞迴在 reports 目錄下搜尋指定檔名 */
    private File findXmlFile(File dir, String fileName) {
        if (!dir.exists()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isDirectory()) {
                File found = findXmlFile(f, fileName);
                if (found != null) return found;
            } else if (f.getName().equals(fileName)) {
                return f;
            }
        }
        return null;
    }
}
