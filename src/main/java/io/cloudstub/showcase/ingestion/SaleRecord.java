package io.cloudstub.showcase.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One row parsed from a sales report CSV. */
public record SaleRecord(
        LocalDate date,
        String product,
        int quantity,
        BigDecimal amount,
        String region,
        String salesperson) {}
