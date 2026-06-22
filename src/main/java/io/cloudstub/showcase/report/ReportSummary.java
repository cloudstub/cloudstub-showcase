package io.cloudstub.showcase.report;

import java.math.BigDecimal;
import java.util.Map;

/** Aggregated statistics for a processed report. */
public record ReportSummary(
        Long reportId,
        long totalRecords,
        long totalQuantity,
        BigDecimal totalAmount,
        Map<String, BigDecimal> amountByRegion,
        Map<String, BigDecimal> amountByProduct) {}
