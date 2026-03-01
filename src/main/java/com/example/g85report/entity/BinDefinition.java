package com.example.g85report.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "BIN_DEFINITION")
public class BinDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "product_id", length = 30)
    private String productId;

    /** G85 BinCode (e.g. 0, 1, 91) */
    @Column(name = "bin_code", nullable = false)
    private Integer binCode;

    /** 'Pass' or 'Fail' */
    @Column(name = "bin_quality", length = 4)
    private String binQuality;

    /** G85 BinDescription */
    @Column(name = "bin_desc", length = 100)
    private String binDesc;
}
