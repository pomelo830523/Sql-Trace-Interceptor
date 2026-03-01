package com.example.g85report.service;

import com.example.g85report.entity.BinDefinition;
import com.example.g85report.entity.DieResult;
import com.example.g85report.entity.WaferInfo;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 將 DB 查出的資料組裝成符合 SEMI G85-1101 格式的 XML 字串。
 */
@Component
public class G85XmlGenerator {

    /**
     * @param wafer   WAFER_INFO 記錄
     * @param dies    該 Wafer 的所有 DIE_RESULT
     * @param binDefs 該 Product 的 BIN_DEFINITION 清單
     * @return G85 XML 字串
     */
    public String generate(WaferInfo wafer, List<DieResult> dies, List<BinDefinition> binDefs) {

        int rows    = wafer.getDieRows();
        int cols    = wafer.getDieCols();
        int nullBin = wafer.getNullBin() != null ? wafer.getNullBin() : 255;

        // ── 1. 建立 rows×cols 二維陣列，預填 nullBin ──────────────────
        int[][] grid = new int[rows][cols];
        for (int[] row : grid) Arrays.fill(row, nullBin);

        for (DieResult die : dies) {
            int r = die.getDieRow();
            int c = die.getDieCol();
            if (r >= 0 && r < rows && c >= 0 && c < cols) {
                grid[r][c] = die.getBinCode();
            }
        }

        // ── 2. 統計每個 Bin 的實際 count ─────────────────────────────
        Map<Integer, Long> binCounts = dies.stream()
                .collect(Collectors.groupingBy(DieResult::getBinCode, Collectors.counting()));

        // ── 3. 組裝 XML ───────────────────────────────────────────────
        StringBuilder sb = new StringBuilder(rows * cols * 5);

        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<Maps>\n\n");

        sb.append("<Map\n")
          .append("xmlns:semi=\"http://www.semi.org\"\n")
          .append("WaferId=\"").append(esc(wafer.getWaferId())).append("\"\n")
          .append("FormatRevision=\"G85-1101\"\n")
          .append(">\n\n\n");

        sb.append("<Device\n")
          .append("ProductId=\"").append(safe(wafer.getProductId())).append("\"\n")
          .append("WaferSize=\"").append(safe(wafer.getWaferSize())).append("\"\n")
          .append("LotId=\"").append(safe(wafer.getLotId())).append("\"\n")
          .append("CreateDate=\"").append(wafer.getCreateDate() != null ? wafer.getCreateDate().toString() : "").append("\"\n")
          .append("SupplierName=\"").append(safe(wafer.getSupplierName())).append("\"\n")
          .append("Rows=\"").append(rows).append("\"\n")
          .append("Columns=\"").append(cols).append("\"\n")
          .append("Orientation=\"").append(wafer.getOrientation() != null ? wafer.getOrientation() : 0).append("\"\n")
          .append("OriginLocation=\"").append(wafer.getOriginLoc() != null ? wafer.getOriginLoc() : 3).append("\"\n")
          .append("BinType=\"Decimal\"\n")
          .append("NullBin=\"").append(nullBin).append("\"\n")
          .append(">\n\n\n");

        // Bin 定義
        for (BinDefinition bin : binDefs) {
            long count = binCounts.getOrDefault(bin.getBinCode(), 0L);
            sb.append("<Bin\n")
              .append("BinCode=\"").append(String.format("%03d", bin.getBinCode())).append("\"\n")
              .append("BinCount=\"").append(count).append("\"\n")
              .append("BinQuality=\"").append(safe(bin.getBinQuality())).append("\"\n")
              .append("BinDescription=\"").append(safe(bin.getBinDesc())).append("\"\n")
              .append("/>\n\n\n");
        }

        // SupplierData
        sb.append("<SupplierData\n")
          .append("ProductCode=\"").append(safe(wafer.getProductCode())).append("\"\n")
          .append("RecipeName=\"").append(safe(wafer.getRecipeName())).append("\"\n")
          .append("ToolType=\"").append(safe(wafer.getToolType())).append("\"\n")
          .append("/>\n\n\n");

        // Data section
        sb.append("<Data MapName=\"").append(safe(wafer.getMapName())).append("\" Version=\"2\">\n");
        for (int r = 0; r < rows; r++) {
            sb.append("<Row><![CDATA[");
            for (int c = 0; c < cols; c++) {
                sb.append(String.format("%03d", grid[r][c]));
                if (c < cols - 1) sb.append(" ");
            }
            sb.append("]]></Row>\n");
        }
        sb.append("</Data>\n\n");

        sb.append("</Device>\n\n");
        sb.append("</Map>\n\n");
        sb.append("</Maps>\n");

        return sb.toString();
    }

    private String safe(String s) {
        return s != null ? esc(s) : "";
    }

    /** XML 屬性值跳脫 */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
