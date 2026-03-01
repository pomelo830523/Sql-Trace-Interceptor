package com.example.g85report.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "WAFER_INFO")
public class WaferInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "wafer_id", unique = true, nullable = false, length = 50)
    private String waferId;

    @Column(name = "lot_id", nullable = false, length = 20)
    private String lotId;

    @Column(name = "product_id", length = 30)
    private String productId;

    @Column(name = "wafer_size", length = 10)
    private String waferSize;

    @Column(name = "supplier_name", length = 50)
    private String supplierName;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    /** 晶圓 Die 行數 (對應 G85 Rows) */
    @Column(name = "die_rows")
    private Integer dieRows;

    /** 晶圓 Die 列數 (對應 G85 Columns) */
    @Column(name = "die_cols")
    private Integer dieCols;

    /** 晶圓方向 0/90/180/270 */
    @Column(name = "orientation")
    private Integer orientation = 0;

    /** 原點位置 1~4 */
    @Column(name = "origin_loc")
    private Integer originLoc = 3;

    /** 無效 Die 的 BinCode (G85 NullBin) */
    @Column(name = "null_bin")
    private Integer nullBin = 255;

    @Column(name = "map_name", length = 50)
    private String mapName;

    @Column(name = "recipe_name", length = 50)
    private String recipeName;

    @Column(name = "product_code", length = 30)
    private String productCode;

    @Column(name = "tool_type", length = 30)
    private String toolType;
}
