package io.cloudstub.showcase.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

/** A single sales transaction parsed from a report's CSV, linked to its {@link Report}. */
@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @Column(name = "sale_date", nullable = false)
    private LocalDate saleDate;

    @Column(nullable = false)
    private String product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String salesperson;

    protected Sale() {}

    public Sale(
            Long reportId,
            LocalDate saleDate,
            String product,
            int quantity,
            BigDecimal amount,
            String region,
            String salesperson) {
        this.reportId = reportId;
        this.saleDate = saleDate;
        this.product = product;
        this.quantity = quantity;
        this.amount = amount;
        this.region = region;
        this.salesperson = salesperson;
    }

    public Long getId() {
        return id;
    }

    public Long getReportId() {
        return reportId;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public String getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getRegion() {
        return region;
    }

    public String getSalesperson() {
        return salesperson;
    }
}
