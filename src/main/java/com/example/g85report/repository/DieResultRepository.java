package com.example.g85report.repository;

import com.example.g85report.entity.DieResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DieResultRepository extends JpaRepository<DieResult, Long> {

    List<DieResult> findByWaferId(String waferId);
}
