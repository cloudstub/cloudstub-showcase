package io.cloudstub.showcase.aws;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * AWS SDK v2 client beans. The clients are configured exactly as a production application would
 * configure them. The only environment-specific input is {@code aws.endpoint-url}: when set (as
 * CloudStub does in tests) the client is redirected there; when empty (production) the client uses
 * the SDK's default AWS endpoints. No mock-specific settings are applied.
 */
@Configuration
public class AwsConfig {

    @Value("${aws.endpoint-url:}")
    private String endpointUrl;

    @Value("${aws.region:us-east-1}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return configure(S3Client.builder()).build();
    }

    @Bean
    public SqsClient sqsClient() {
        return configure(SqsClient.builder()).build();
    }

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return configure(SecretsManagerClient.builder()).build();
    }

    private <C, B extends AwsClientBuilder<B, C>> B configure(B builder) {
        builder.region(Region.of(region));
        if (!endpointUrl.isBlank()) {
            builder.endpointOverride(URI.create(endpointUrl));
        }
        return builder;
    }
}
