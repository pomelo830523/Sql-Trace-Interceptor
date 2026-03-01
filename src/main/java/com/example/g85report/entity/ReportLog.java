package com.example.g85report.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "REPORT_LOG")
public class ReportLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", length = 36)
    private String reportId;

    @Column(name = "lot_id", length = 20)
    private String lotId;

    /** 'SUCCESS' or 'FAIL' */
    @Column(name = "status", length = 10)
    private String status;

    @Column(name = "wafer_count")
    private Integer waferCount;

    @Column(name = "output_path", length = 500)
    private String outputPath;

    @Column(name = "error_msg", length = 500)
    private String errorMsg;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
