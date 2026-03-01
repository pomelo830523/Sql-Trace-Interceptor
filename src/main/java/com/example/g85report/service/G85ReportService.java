package com.example.g85report.service;

import com.example.g85report.config.SqlCaptureHolder;
import com.example.g85report.dto.GenerateReportRequest;
import com.example.g85report.dto.GenerateReportResponse;
import com.example.g85report.dto.GenerateWithTraceResponse;
import com.example.g85report.dto.SqlTraceStep;
import com.example.g85report.dto.WaferSqlTrace;
import com.example.g85report.entity.BinDefinition;
import com.example.g85report.entity.DieResult;
import com.example.g85report.entity.ReportLog;
import com.example.g85report.entity.WaferInfo;
import com.example.g85report.repository.BinDefinitionRepository;
import com.example.g85report.repository.DieResultRepository;
import com.example.g85report.repository.ReportLogRepository;
import com.example.g85report.repository.WaferInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class G85ReportService {

    private final WaferInfoRepository    waferInfoRepo;
    private final DieResultRepository    dieResultRepo;
    private final BinDefinitionRepository binDefRepo;
    private final ReportLogRepository    reportLogRepo;
    private final G85XmlGenerator        xmlGenerator;

    @Value("${report.output.base-path:./reports}")
    private String outputBasePath;

    /**
     * 依 lotId 產出整個 Lot 所有 Wafer 的 G85 XML，
     * 存入 {outputBasePath}/{lotId}/ 目錄下。
     */
    public GenerateReportResponse generate(GenerateReportRequest request) {

        String reportId = UUID.randomUUID().toString();
        String lotId    = request.getLotId();
        String outputDir = outputBasePath + "/" + lotId + "/";

        List<String> fileNames = new ArrayList<>();
        String status   = "SUCCESS";
        String errorMsg = null;

        try {
            // ── 1. 查出該 Lot 所有 Wafer ──────────────────────────────
            List<WaferInfo> wafers = waferInfoRepo.findByLotId(lotId);
            if (wafers.isEmpty()) {
                throw new IllegalArgumentException("LotId not found: " + lotId);
            }

            // ── 2. 建立輸出目錄 ───────────────────────────────────────
            Path dirPath = Paths.get(outputDir);
            Files.createDirectories(dirPath);

            for (WaferInfo wafer : wafers) {

                // ── 3. 查 Die 測試結果 ────────────────────────────────
                List<DieResult> dies = dieResultRepo.findByWaferId(wafer.getWaferId());

                // ── 4. 查 Bin 定義（依 productId） ────────────────────
                List<BinDefinition> binDefs = binDefRepo.findByProductIdOrderByBinCode(wafer.getProductId());

                // ── 5. 產生 G85 XML ───────────────────────────────────
                String xml = xmlGenerator.generate(wafer, dies, binDefs);

                // ── 6. 寫入檔案 ───────────────────────────────────────
                String fileName = wafer.getWaferId() + ".xml";
                Files.writeString(dirPath.resolve(fileName), xml);
                fileNames.add(fileName);

                log.info("[G85] Wafer {} generated, dies={}", wafer.getWaferId(), dies.size());
            }

            log.info("[G85] Lot {} done, total {} wafers", lotId, fileNames.size());

        } catch (Exception e) {
            log.error("[G85] Lot {} generation failed: {}", lotId, e.getMessage(), e);
            status   = "FAIL";
            errorMsg = e.getMessage();
        }

        // ── 7. 寫入報表紀錄 ────────────────────────────────────────────
        ReportLog record = new ReportLog();
        record.setReportId(reportId);
        record.setLotId(lotId);
        record.setStatus(status);
        record.setWaferCount(fileNames.size());
        record.setOutputPath(outputDir);
        record.setErrorMsg(errorMsg);
        record.setCreatedAt(LocalDateTime.now());
        reportLogRepo.save(record);

        return GenerateReportResponse.builder()
                .reportId(reportId)
                .lotId(lotId)
                .waferCount(fileNames.size())
                .status(status)
                .outputPath(outputDir)
                .files(fileNames)
                .errorMsg(errorMsg)
                .build();
    }

    public List<ReportLog> getHistory(String lotId) {
        return reportLogRepo.findByLotIdOrderByCreatedAtDesc(lotId);
    }

    /**
     * Same as {@link #generate} but also returns per-wafer SQL execution traces
     * with real parameter values. SQL is captured in ThreadLocal and never persisted.
     */
    public GenerateWithTraceResponse generateWithTrace(GenerateReportRequest request) {

        String reportId  = UUID.randomUUID().toString();
        String lotId     = request.getLotId();
        String outputDir = outputBasePath + "/" + lotId + "/";

        List<String>       fileNames = new ArrayList<>();
        List<WaferSqlTrace> sqlTrace = new ArrayList<>();
        String status   = "SUCCESS";
        String errorMsg = null;

        try {
            // ── Step 1: capture findByLotId SQL ───────────────────────────
            SqlCaptureHolder.startCapture();
            List<WaferInfo> wafers = waferInfoRepo.findByLotId(lotId);
            List<String> lotSqls = new ArrayList<>(SqlCaptureHolder.getCaptured());
            SqlCaptureHolder.stopCapture();

            if (wafers.isEmpty()) {
                throw new IllegalArgumentException("LotId not found: " + lotId);
            }

            // ── Create output directory ────────────────────────────────────
            Path dirPath = Paths.get(outputDir);
            Files.createDirectories(dirPath);

            // ── Steps 2-3: per-wafer queries + XML generation ─────────────
            for (WaferInfo wafer : wafers) {
                SqlCaptureHolder.startCapture();
                try {
                    List<DieResult>    dies    = dieResultRepo.findByWaferId(wafer.getWaferId());
                    List<BinDefinition> binDefs = binDefRepo.findByProductIdOrderByBinCode(wafer.getProductId());

                    String xml      = xmlGenerator.generate(wafer, dies, binDefs);
                    String fileName = wafer.getWaferId() + ".xml";
                    Files.writeString(dirPath.resolve(fileName), xml);
                    fileNames.add(fileName);

                    // Merge lotSqls (step 1) + per-wafer sqls (steps 2-3)
                    List<String> waferSqls = SqlCaptureHolder.getCaptured();
                    List<String> allSqls   = new ArrayList<>(lotSqls);
                    allSqls.addAll(waferSqls);

                    sqlTrace.add(buildWaferTrace(wafer.getWaferId(), allSqls));

                    log.info("[G85-trace] Wafer {} generated, dies={}", wafer.getWaferId(), dies.size());
                } finally {
                    SqlCaptureHolder.stopCapture();
                }
            }

            log.info("[G85-trace] Lot {} done, total {} wafers", lotId, fileNames.size());

        } catch (Exception e) {
            log.error("[G85-trace] Lot {} generation failed: {}", lotId, e.getMessage(), e);
            // Ensure ThreadLocal is cleaned up on unexpected error
            SqlCaptureHolder.stopCapture();
            status   = "FAIL";
            errorMsg = e.getMessage();
        }

        // ── Persist report log (same as generate) ─────────────────────────
        ReportLog record = new ReportLog();
        record.setReportId(reportId);
        record.setLotId(lotId);
        record.setStatus(status);
        record.setWaferCount(fileNames.size());
        record.setOutputPath(outputDir);
        record.setErrorMsg(errorMsg);
        record.setCreatedAt(LocalDateTime.now());
        reportLogRepo.save(record);

        return GenerateWithTraceResponse.builder()
                .reportId(reportId)
                .lotId(lotId)
                .waferCount(fileNames.size())
                .status(status)
                .outputPath(outputDir)
                .files(fileNames)
                .errorMsg(errorMsg)
                .sqlTrace(sqlTrace)
                .build();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private WaferSqlTrace buildWaferTrace(String waferId, List<String> sqls) {
        List<SqlTraceStep> steps = new ArrayList<>();
        for (int i = 0; i < sqls.size(); i++) {
            String sql = sqls.get(i);
            steps.add(SqlTraceStep.builder()
                    .stepOrder(i + 1)
                    .sqlType(getSqlType(sql))
                    .sql(sql)
                    .explanation(getExplanation(sql))
                    .build());
        }
        return WaferSqlTrace.builder()
                .waferId(waferId)
                .steps(steps)
                .build();
    }

    private String getSqlType(String sql) {
        String lower = sql.toLowerCase();
        if (lower.contains("wafer_info"))     return "WAFER_INFO";
        if (lower.contains("die_result"))     return "DIE_RESULT";
        if (lower.contains("bin_definition")) return "BIN_DEFINITION";
        return "UNKNOWN";
    }

    private String getExplanation(String sql) {
        String lower = sql.toLowerCase();
        if (lower.contains("wafer_info")) {
            return "依批號（lot_id）查詢該批次內所有晶圓的基本資訊，包含產品代號、晶圓尺寸、Die 格數等，是整份報表產生流程的起點。";
        }
        if (lower.contains("die_result")) {
            return "依晶圓 ID（wafer_id）查詢該晶圓所有 Die 的座標（row, col）與 Bin 分類碼（bin_code），用於填入 G85 XML 的晶圓格點陣列。";
        }
        if (lower.contains("bin_definition")) {
            return "依產品代號（product_id）查詢所有 Bin 碼的品質分類（Pass/Fail）與描述文字，用於在 G85 XML 輸出 <Bin> 標籤。";
        }
        return "";
    }
}
