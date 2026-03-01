package com.example.g85report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryResultTable {

    /** Column names, in the order they appear in each row. */
    private List<String> columns;

    /** Each inner list is one row; values are in the same order as columns. */
    private List<List<Object>> rows;
}
