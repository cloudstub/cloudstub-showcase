package io.cloudstub.showcase.report;

import io.cloudstub.showcase.aws.AwsResources;
import io.cloudstub.showcase.ingestion.ReportMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import tools.jackson.databind.ObjectMapper;

/** Orchestrates report upload: store the raw CSV in S3, then queue it for ingestion. */
@Service
public class ReportService {

    private final S3Client s3;
    private final SqsClient sqs;
    private final AwsResources aws;
    private final ReportRepository reports;
    private final SaleRepository sales;
    private final ObjectMapper mapper;

    public ReportService(
            S3Client s3,
            SqsClient sqs,
            AwsResources aws,
            ReportRepository reports,
            SaleRepository sales,
            ObjectMapper mapper) {
        this.s3 = s3;
        this.sqs = sqs;
        this.aws = aws;
        this.reports = reports;
        this.sales = sales;
        this.mapper = mapper;
    }

    /** Stores the CSV in S3, records a PENDING report, and publishes an ingestion message. */
    public Report upload(String filename, byte[] content) {
        aws.ensureBucket();
        String key = "reports/" + UUID.randomUUID() + "-" + filename;
        s3.putObject(b -> b.bucket(aws.bucket()).key(key), RequestBody.fromBytes(content));

        Report report =
                reports.save(new Report(filename, key, ReportStatus.PENDING, Instant.now()));

        sqs.sendMessage(
                b ->
                        b.queueUrl(aws.queueUrl())
                                .messageBody(serialize(new ReportMessage(report.getId(), key))));
        return report;
    }

    public Report get(Long id) {
        return reports
                .findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No report with id " + id));
    }

    public ReportSummary summarize(Long id) {
        Report report = get(id);
        List<Sale> rows = sales.findByReportId(report.getId());

        long totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        Map<String, BigDecimal> byRegion = new LinkedHashMap<>();
        Map<String, BigDecimal> byProduct = new LinkedHashMap<>();
        for (Sale s : rows) {
            totalQuantity += s.getQuantity();
            totalAmount = totalAmount.add(s.getAmount());
            byRegion.merge(s.getRegion(), s.getAmount(), BigDecimal::add);
            byProduct.merge(s.getProduct(), s.getAmount(), BigDecimal::add);
        }
        return new ReportSummary(
                report.getId(), rows.size(), totalQuantity, totalAmount, byRegion, byProduct);
    }

    private String serialize(ReportMessage message) {
        try {
            return mapper.writeValueAsString(message);
        } catch (Exception e) {
            throw new IllegalStateException("Could not serialize ingestion message", e);
        }
    }
}
