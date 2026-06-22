# cloudstub-showcase

A sales report ingestion service that demonstrates using CloudStub to test an application written
against real AWS, with no local AWS infrastructure.

## Domain

Regional sales teams upload daily CSV reports of sales transactions (date, product, quantity,
amount, region, salesperson). The service stores the raw file, parses and persists the rows, and
exposes an API to query processing status and aggregated statistics.

## Flow

1. `POST /reports/upload` accepts a CSV file, stores it in S3, and publishes an SQS message
   referencing the file.
2. `IngestionProcessor` consumes the SQS message, fetches the CSV from S3, parses it, persists the
   rows to the database, and advances the report status.
3. `GET /reports/{id}/status` returns the processing status (`PENDING` → `PROCESSING` → `DONE` /
   `FAILED`).
4. `GET /reports/{id}/summary` returns aggregated statistics (totals, by region, by product).

Database credentials are held in Secrets Manager and read by `DatabaseCredentialsProvider`.

## AWS services

| Service         | Use                                            |
| --------------- | ---------------------------------------------- |
| S3              | Stores the uploaded CSV files                  |
| SQS             | Queues uploaded reports for ingestion          |
| Secrets Manager | Holds the database credentials                 |

The AWS clients (`AwsConfig`) are configured as a production application would configure them. The
only environment-specific input is `aws.endpoint-url`, which CloudStub sets in tests to redirect AWS
traffic to its embedded mock.

## Build and test

```
./gradlew test
```

The integration test (`ReportIngestionIntegrationTest`) boots the application with
`CloudStubExtension`, which starts CloudStub and serves S3, SQS, and Secrets Manager in-process. It
uses PostgreSQL in production and H2 under the `test` profile. CloudStub is a test-only dependency,
consumed from published Maven artifacts.

## Run

The application uses PostgreSQL and real AWS in production. Provide a database, a Secrets Manager
secret named `showcase/db-credentials` (a JSON document with `username` and `password`), and the S3
bucket / SQS queue named in `application.yml`, then:

```
./gradlew bootRun
```
