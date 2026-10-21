# Payment Reconciliation Engine

Spring Boot + Spring Batch service for reconciling internal transactions with third-party transaction mutations from CSV files.

## Tech Stack

- Java 25
- Spring Boot 3.5.16
- Spring Batch
- Spring Data JPA
- PostgreSQL
- Maven

## Main Features

- Read third-party transaction data from CSV files
- Match transactions with internal transactions based on `referenceId`
- Reconciliation result status:
  - `MATCHED`
  - `SUSPECT`
  - `UNMATCHED`
- Save reconciliation results into `reconciliation_results` table
- Trigger batch job manually using REST API

## Important Structure

- App entry: [src/main/java/com/fintech/reconcile/PaymentReconciliationApplication.java](src/main/java/com/fintech/reconcile/PaymentReconciliationApplication.java)
- Batch config: [src/main/java/com/fintech/reconcile/config/BatchConfig.java](src/main/java/com/fintech/reconcile/config/BatchConfig.java)
- Processor: [src/main/java/com/fintech/reconcile/batch/processor/ReconciliationItemProcessor.java](src/main/java/com/fintech/reconcile/batch/processor/ReconciliationItemProcessor.java)
- Trigger API: [src/main/java/com/fintech/reconcile/controller/ReconciliationTriggerController.java](src/main/java/com/fintech/reconcile/controller/ReconciliationTriggerController.java)
- Application config: [src/main/resources/application.yml](src/main/resources/application.yml)

## Prerequisites

- JDK 25
- PostgreSQL is running
- Maven 3.9+

## Configuration

Configuration can be changed in [src/main/resources/application.yml](src/main/resources/application.yml).

Some important properties:

- `server.port`: default is `8481`
- `spring.datasource.*`: database connection settings
- `app.reconciliation.input-file-path`: default is `data/input/third_party_mutation.csv`
- `app.reconciliation.chunk-size`: default is `500`

Make sure the input CSV file is available in the configured path before running the reconciliation job.

## Input CSV Format

The CSV reader uses these headers:

- `referenceId`
- `accountNumber`
- `amount`
- `transactionType`
- `transactionTime`

Example:

```csv
referenceId,accountNumber,amount,transactionType,transactionTime
REF-001,1234567890,125000.00,DEBIT,2026-10-21T10:15:00
```

## Running the Application

Run the application using Maven:

```bash
mvn clean spring-boot:run
```

## Running Batch Job via API

The batch job can be triggered manually using this endpoint:

- `POST /api/reconciliation/run`

Example using curl:

```bash
curl -X POST http://localhost:8481/api/reconciliation/run
```

Example response:

```json
{
  "jobExecutionid": 12,
  "status": "STARTED",
  "startTime": "2026-10-21T16:20:11.140"
}
```

## Build and Test

To compile the test code:

```bash
mvn clean test-compile
```

To run the tests:

```bash
mvn clean test
```

## Notes

- Currently, [src/main/resources/application.yml](src/main/resources/application.yml) contains database credentials in plain text. For team or production usage, it is better to move these credentials into environment variables.
- `spring.batch.job.enabled=false` means the batch job will not run automatically when the application starts. The job only runs when triggered through the API.