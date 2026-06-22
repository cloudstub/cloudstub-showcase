package io.cloudstub.showcase.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Resolves the report bucket and ingestion queue, creating them on first use. Production code would
 * typically provision these out of band; creating them lazily keeps the application self-contained
 * for local and test runs.
 */
@Component
public class AwsResources {

    private final S3Client s3;
    private final SqsClient sqs;
    private final String bucket;
    private final String queueName;

    private volatile boolean bucketReady;
    private volatile String queueUrl;

    public AwsResources(
            S3Client s3,
            SqsClient sqs,
            @Value("${showcase.s3-bucket}") String bucket,
            @Value("${showcase.sqs-queue}") String queueName) {
        this.s3 = s3;
        this.sqs = sqs;
        this.bucket = bucket;
        this.queueName = queueName;
    }

    public String bucket() {
        return bucket;
    }

    public void ensureBucket() {
        if (bucketReady) {
            return;
        }
        try {
            s3.createBucket(b -> b.bucket(bucket));
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException ignored) {
            // Already provisioned.
        }
        bucketReady = true;
    }

    public String queueUrl() {
        String url = queueUrl;
        if (url == null) {
            url = sqs.createQueue(b -> b.queueName(queueName)).queueUrl();
            queueUrl = url;
        }
        return url;
    }
}
