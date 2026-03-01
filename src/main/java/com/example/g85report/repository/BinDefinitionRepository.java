package com.example.g85report.repository;

import com.example.g85report.entity.BinDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BinDefinitionRepository extends JpaRepository<BinDefinition, Integer> {

    List<BinDefinition> findByProductIdOrderByBinCode(String productId);
}
