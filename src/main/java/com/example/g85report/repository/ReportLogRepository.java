package com.example.g85report.repository;

import com.example.g85report.entity.ReportLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportLogRepository extends JpaRepository<ReportLog, Long> {

    List<ReportLog> findByLotIdOrderByCreatedAtDesc(String lotId);
}
