package com.example.g85report.service;

import com.example.g85report.dto.GenerateReportRequest;
import com.example.g85report.dto.GenerateReportResponse;
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
}
