package com.example.g85report.service;

import com.example.g85report.dto.GenerateReportRequest;
import com.example.g85report.dto.GenerateReportResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Converts a flat list of captured SQLs into per-wafer trace objects.
     * Called by the controller when X-Trace-SQL: true was present.
     *
     * Expected layout after filtering UNKNOWN entries:
     *   [0]           WAFER_INFO  – shared across all wafers
     *   [1], [2]      DIE_RESULT + BIN_DEFINITION for wafer 1
     *   [3], [4]      DIE_RESULT + BIN_DEFINITION for wafer 2
     *   ...
     */
    public List<WaferSqlTrace> buildTrace(List<String> sqls) {
        // 1. Drop entries that don't map to a known table (e.g. INSERT report_log)
        List<String> filtered = sqls.stream()
                .filter(s -> !"UNKNOWN".equals(getSqlType(s)))
                .toList();

        List<WaferSqlTrace> result = new ArrayList<>();
        if (filtered.isEmpty()) return result;

        // 2. First entry is WAFER_INFO – shared step 1 for every wafer
        SqlTraceStep step1 = SqlTraceStep.builder()
                .stepOrder(1)
                .sqlType(getSqlType(filtered.get(0)))
                .sql(filtered.get(0))
                .explanation(getExplanation(filtered.get(0)))
                .build();

        // 3. Remaining entries are paired: DIE_RESULT + BIN_DEFINITION
        Pattern waferIdPattern = Pattern.compile("wafer_id='([^']+)'", Pattern.CASE_INSENSITIVE);
        for (int i = 1; i + 1 < filtered.size(); i += 2) {
            String dieResultSql = filtered.get(i);
            String binDefSql    = filtered.get(i + 1);

            Matcher m = waferIdPattern.matcher(dieResultSql);
            String waferId = m.find() ? m.group(1) : "UNKNOWN-" + ((i / 2) + 1);

            SqlTraceStep step2 = SqlTraceStep.builder()
                    .stepOrder(2)
                    .sqlType(getSqlType(dieResultSql))
                    .sql(dieResultSql)
                    .explanation(getExplanation(dieResultSql))
                    .build();

            SqlTraceStep step3 = SqlTraceStep.builder()
                    .stepOrder(3)
                    .sqlType(getSqlType(binDefSql))
                    .sql(binDefSql)
                    .explanation(getExplanation(binDefSql))
                    .build();

            result.add(WaferSqlTrace.builder()
                    .waferId(waferId)
                    .steps(List.of(step1, step2, step3))
                    .build());
        }
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

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
