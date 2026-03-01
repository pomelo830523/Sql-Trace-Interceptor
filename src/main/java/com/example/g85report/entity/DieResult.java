package com.example.g85report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "DIE_RESULT", indexes = {
    @Index(name = "idx_die_result_wafer_id", columnList = "wafer_id")
})
public class DieResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → WAFER_INFO.wafer_id */
    @Column(name = "wafer_id", nullable = false, length = 50)
    private String waferId;

    /** 0-based 行位置 */
    @Column(name = "die_row", nullable = false)
    private Integer dieRow;

    /** 0-based 列位置 */
    @Column(name = "die_col", nullable = false)
    private Integer dieCol;

    /** 對應 BIN_DEFINITION.bin_code */
    @Column(name = "bin_code", nullable = false)
    private Integer binCode;
}
