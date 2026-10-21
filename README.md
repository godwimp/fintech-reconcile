# Payment Reconciliation Engine

Spring Boot + Spring Batch service untuk rekonsiliasi transaksi internal vs mutasi pihak ketiga (CSV).

## Tech stack

- Java 25
- Spring Boot 3.5.16
- Spring Batch
- Spring Data JPA
- PostgreSQL
- Maven

## Fitur utama

- Reader CSV untuk mutasi pihak ketiga
- Matching ke transaksi internal berdasarkan `referenceId`
- Status hasil rekonsiliasi:
  - `MATCHED`
  - `SUSPECT`
  - `UNMATCHED`
- Simpan hasil ke tabel `reconciliation_results`
- Trigger job manual via REST API

## Struktur penting

- App entry: [src/main/java/com/fintech/reconcile/PaymentReconciliationApplication.java](src/main/java/com/fintech/reconcile/PaymentReconciliationApplication.java)
- Batch config: [src/main/java/com/fintech/reconcile/config/BatchConfig.java](src/main/java/com/fintech/reconcile/config/BatchConfig.java)
- Processor: [src/main/java/com/fintech/reconcile/batch/processor/ReconciliationItemProcessor.java](src/main/java/com/fintech/reconcile/batch/processor/ReconciliationItemProcessor.java)
- Trigger API: [src/main/java/com/fintech/reconcile/controller/ReconciliationTriggerController.java](src/main/java/com/fintech/reconcile/controller/ReconciliationTriggerController.java)
- Config: [src/main/resources/application.yml](src/main/resources/application.yml)

## Prerequisites

- JDK 25
- PostgreSQL aktif
- Maven 3.9+

## Konfigurasi

Sesuaikan di [src/main/resources/application.yml](src/main/resources/application.yml):

- `server.port` default `8481`
- `spring.datasource.*`
- `app.reconciliation.input-file-path` default `data/input/third_party_mutation.csv`
- `app.reconciliation.chunk-size` default `500`

Pastikan file input CSV tersedia di path yang dikonfigurasi.

## Format CSV input

Header yang dipakai reader:

- `referenceId`
- `accountNumber`
- `amount`
- `transactionType`
- `transactionTime`

Contoh:

```csv
referenceId,accountNumber,amount,transactionType,transactionTime
REF-001,1234567890,125000.00,DEBIT,2026-10-21T10:15:00
```

## Menjalankan aplikasi

```bash
mvn clean spring-boot:run
```

## Menjalankan batch job via API

Endpoint:

- `POST /api/reconciliation/run`

Contoh:

```bash
curl -X POST http://localhost:8481/api/reconciliation/run
```

Contoh response:

```json
{
  "jobExecutionid": 12,
  "status": "STARTED",
  "startTime": "2026-10-21T16:20:11.140"
}
```

## Build dan test

```bash
mvn clean test-compile
mvn clean test
```

## Catatan

- Saat ini [src/main/resources/application.yml](src/main/resources/application.yml) berisi kredensial database plain text. Ganti dengan environment variable untuk pemakaian tim/production.
- `spring.batch.job.enabled=false` artinya job tidak auto-run saat startup, hanya jalan ketika dipicu API.
