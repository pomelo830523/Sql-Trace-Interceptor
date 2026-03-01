package com.example.g85report.repository;

import com.example.g85report.entity.WaferInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WaferInfoRepository extends JpaRepository<WaferInfo, Long> {

    List<WaferInfo> findByLotId(String lotId);

    Optional<WaferInfo> findByWaferId(String waferId);

    boolean existsByLotId(String lotId);
}
