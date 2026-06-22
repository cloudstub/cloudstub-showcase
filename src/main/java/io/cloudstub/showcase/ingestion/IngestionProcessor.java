package io.cloudstub.showcase.ingestion;

import io.cloudstub.showcase.aws.AwsResources;
import io.cloudstub.showcase.report.Report;
import io.cloudstub.showcase.report.ReportRepository;
import io.cloudstub.showcase.report.ReportStatus;
import io.cloudstub.showcase.report.Sale;
import io.cloudstub.showcase.report.SaleRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.ObjectMapper;

/**
 * Drains the ingestion queue: for each message it fetches the CSV from S3, parses it, persists the
 * rows, and advances the report's status (PROCESSING then DONE, or FAILED on error).
 */
@Component
public class IngestionProcessor {

    private static final Logger log = LoggerFactory.getLogger(IngestionProcessor.class);

    private final S3Client s3;
    private final SqsClient sqs;
    private final AwsResources aws;
    private final CsvParser parser;
    private final ReportRepository reports;
    private final SaleRepository sales;
    private final ObjectMapper mapper;

    public IngestionProcessor(
            S3Client s3,
            SqsClient sqs,
            AwsResources aws,
            CsvParser parser,
            ReportRepository reports,
            SaleRepository sales,
            ObjectMapper mapper) {
        this.s3 = s3;
        this.sqs = sqs;
        this.aws = aws;
        this.parser = parser;
        this.reports = reports;
        this.sales = sales;
        this.mapper = mapper;
    }

    /** Processes all currently queued messages and returns how many were handled. */
    public int processPending() {
        String queueUrl = aws.queueUrl();
        int handled = 0;
        while (true) {
            List<Message> messages =
                    sqs.receiveMessage(
                                    b ->
                                            b.queueUrl(queueUrl)
                                                    .maxNumberOfMessages(10)
                                                    .waitTimeSeconds(0))
                            .messages();
            if (messages.isEmpty()) {
                break;
            }
            for (Message message : messages) {
                handle(message);
                sqs.deleteMessage(b -> b.queueUrl(queueUrl).receiptHandle(message.receiptHandle()));
                handled++;
            }
        }
        return handled;
    }

    private void handle(Message message) {
        ReportMessage ref;
        try {
            ref = mapper.readValue(message.body(), ReportMessage.class);
        } catch (Exception e) {
            log.error("Skipping unparseable ingestion message: {}", message.body(), e);
            return;
        }

        Report report = reports.findById(ref.reportId()).orElse(null);
        if (report == null) {
            log.error("Ingestion message references unknown report {}", ref.reportId());
            return;
        }

        try {
            report.setStatus(ReportStatus.PROCESSING);
            reports.save(report);

            String csv =
                    s3.getObjectAsBytes(b -> b.bucket(aws.bucket()).key(ref.s3Key())).asUtf8String();
            for (SaleRecord record : parser.parse(csv)) {
                sales.save(
                        new Sale(
                                report.getId(),
                                record.date(),
                                record.product(),
                                record.quantity(),
                                record.amount(),
                                record.region(),
                                record.salesperson()));
            }

            report.setStatus(ReportStatus.DONE);
            reports.save(report);
        } catch (Exception e) {
            log.error("Ingestion failed for report {}", report.getId(), e);
            report.setStatus(ReportStatus.FAILED);
            reports.save(report);
        }
    }
}
