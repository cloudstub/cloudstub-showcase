package io.cloudstub.showcase.ingestion;

/** SQS message payload referencing an uploaded report awaiting ingestion. */
public record ReportMessage(Long reportId, String s3Key) {}
