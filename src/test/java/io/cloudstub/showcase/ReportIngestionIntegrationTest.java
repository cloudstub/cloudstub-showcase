package io.cloudstub.showcase;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import io.cloudstub.junit.CloudStubExtension;
import io.cloudstub.showcase.aws.DatabaseCredentialsProvider;
import io.cloudstub.showcase.aws.DbCredentials;
import io.cloudstub.showcase.ingestion.IngestionProcessor;
import io.cloudstub.showcase.report.ReportController;
import io.cloudstub.showcase.report.ReportStatus;
import io.cloudstub.showcase.report.ReportSummary;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

/**
 * Drives the full ingestion flow (upload to S3, queue on SQS, process, persist, query) and the
 * Secrets Manager credential lookup against CloudStub. The application is wired exactly as it would
 * be for real AWS; the only difference is that CloudStubExtension redirects AWS traffic to the
 * embedded mock via the {@code aws.endpoint-url} system property.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@DirtiesContext
class ReportIngestionIntegrationTest {

    @RegisterExtension static final CloudStubExtension cloudMock = new CloudStubExtension();

    private static final String CSV =
            """
            date,product,quantity,amount,region,salesperson
            2026-01-05,Widget,10,100.00,EMEA,Alice
            2026-01-05,Gadget,5,250.00,AMER,Bob
            2026-01-06,Widget,3,30.00,AMER,Bob
            """;

    @Autowired ReportController controller;
    @Autowired IngestionProcessor processor;
    @Autowired SecretsManagerClient secretsManager;
    @Autowired DatabaseCredentialsProvider databaseCredentials;

    @Test
    void uploadIsProcessedAndSummarized() throws Exception {
        // Upload: stored in S3, report recorded PENDING, message queued on SQS.
        MockMultipartFile file =
                new MockMultipartFile("file", "sales.csv", "text/csv", CSV.getBytes(UTF_8));
        Map<String, Object> uploaded = controller.upload(file);
        long reportId = (long) uploaded.get("id");
        assertThat(uploaded.get("status")).isEqualTo(ReportStatus.PENDING);

        // Process the queue (what the scheduler does in a running app).
        int handled = processor.processPending();
        assertThat(handled).isEqualTo(1);

        // Status reflects completion.
        assertThat(controller.status(reportId).get("status")).isEqualTo(ReportStatus.DONE);

        // Summary reflects the persisted rows.
        ReportSummary summary = controller.summary(reportId);
        assertThat(summary.totalRecords()).isEqualTo(3);
        assertThat(summary.totalQuantity()).isEqualTo(18);
        assertThat(summary.totalAmount()).isEqualByComparingTo("380.00");
        assertThat(summary.amountByRegion().get("EMEA")).isEqualByComparingTo("100.00");
        assertThat(summary.amountByRegion().get("AMER")).isEqualByComparingTo("280.00");
        assertThat(summary.amountByProduct().get("Widget")).isEqualByComparingTo("130.00");
        assertThat(summary.amountByProduct().get("Gadget")).isEqualByComparingTo("250.00");
    }

    @Test
    void databaseCredentialsAreReadFromSecretsManager() {
        secretsManager.createSecret(
                b ->
                        b.name("showcase/db-credentials")
                                .secretString("{\"username\":\"showcase\",\"password\":\"s3cr3t\"}"));

        DbCredentials credentials = databaseCredentials.fetch();

        assertThat(credentials.username()).isEqualTo("showcase");
        assertThat(credentials.password()).isEqualTo("s3cr3t");
    }
}
