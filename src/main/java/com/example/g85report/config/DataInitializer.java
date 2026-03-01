package com.example.g85report.config;

import com.example.g85report.entity.BinDefinition;
import com.example.g85report.entity.DieResult;
import com.example.g85report.entity.WaferInfo;
import com.example.g85report.repository.BinDefinitionRepository;
import com.example.g85report.repository.DieResultRepository;
import com.example.g85report.repository.WaferInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 啟動時若 DB 尚無資料，自動插入 25 Wafer × 最多 300 Die 的模擬測試資料。
 * 模擬 Lot: 999999, Product: PROD-001, 晶圓尺寸: 300mm
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final WaferInfoRepository    waferInfoRepo;
    private final DieResultRepository    dieResultRepo;
    private final BinDefinitionRepository binDefRepo;

    private static final String LOT_ID     = "999999";
    private static final String PRODUCT_ID = "PROD-001";
    private static final int    WAFER_CNT  = 25;
    private static final int    DIE_ROWS   = 20;
    private static final int    DIE_COLS   = 15;   // 20×15 = 300 die positions per wafer

    @Override
    public void run(String... args) {
        if (waferInfoRepo.existsByLotId(LOT_ID)) {
            log.info("[DataInitializer] Data already exists, skipping initialization.");
            return;
        }

        log.info("[DataInitializer] Inserting sample data for Lot {}...", LOT_ID);

        // ── 1. Bin 定義 ────────────────────────────────────────────────
        List<BinDefinition> binDefs = List.of(
            new BinDefinition(null, PRODUCT_ID,  0, "Fail", "No Pick Site"),
            new BinDefinition(null, PRODUCT_ID,  1, "Pass", "Pick"),
            new BinDefinition(null, PRODUCT_ID, 91, "Fail", "Ugly"),
            new BinDefinition(null, PRODUCT_ID, 92, "Fail", "Electrical Fail")
        );
        binDefRepo.saveAll(binDefs);

        // ── 2. 25 張 Wafer ─────────────────────────────────────────────
        Random rng = new Random(42);

        for (int w = 1; w <= WAFER_CNT; w++) {
            String waferId = String.format("%s-%04d", LOT_ID, w);

            WaferInfo wafer = new WaferInfo();
            wafer.setWaferId(waferId);
            wafer.setLotId(LOT_ID);
            wafer.setProductId(PRODUCT_ID);
            wafer.setWaferSize("300");
            wafer.setSupplierName("SupplierA");
            wafer.setCreateDate(LocalDateTime.now().minusDays(WAFER_CNT - w));
            wafer.setDieRows(DIE_ROWS);
            wafer.setDieCols(DIE_COLS);
            wafer.setOrientation(0);
            wafer.setOriginLoc(3);
            wafer.setNullBin(255);
            wafer.setMapName("WAFER_MAP");
            wafer.setRecipeName("RECIPE-A");
            wafer.setProductCode("P001");
            wafer.setToolType("PROBER-X");
            waferInfoRepo.save(wafer);

            // ── 3. 每張 Wafer 的 Die 測試結果 ──────────────────────────
            // 模擬圓形晶圓：邊角的 Die 為 null(255)，中間區域有測試結果
            List<DieResult> dies = new ArrayList<>();
            double centerR = DIE_ROWS / 2.0;
            double centerC = DIE_COLS / 2.0;
            double radius  = Math.min(centerR, centerC) - 0.5;

            for (int r = 0; r < DIE_ROWS; r++) {
                for (int c = 0; c < DIE_COLS; c++) {
                    double dist = Math.sqrt(Math.pow(r - centerR + 0.5, 2) + Math.pow(c - centerC + 0.5, 2));
                    if (dist > radius) continue;  // 圓外 → NullBin, 不寫入 DB

                    int binCode = pickBin(rng);
                    dies.add(new DieResult(null, waferId, r, c, binCode));
                }
            }
            dieResultRepo.saveAll(dies);
            log.info("[DataInitializer] Wafer {} inserted, dies={}", waferId, dies.size());
        }

        log.info("[DataInitializer] Done. {} wafers initialized.", WAFER_CNT);
    }

    /**
     * 模擬測試結果分佈：
     * 85% Pass(1), 10% Ugly(91), 3% Electrical Fail(92), 2% No Pick(0)
     */
    private int pickBin(Random rng) {
        int r = rng.nextInt(100);
        if (r < 85) return 1;
        if (r < 95) return 91;
        if (r < 98) return 92;
        return 0;
    }
}
