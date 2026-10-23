package com.fintech.reconcile.config;

import com.fintech.reconcile.batch.listener.ReconciliationJobExecutionListener;
import com.fintech.reconcile.batch.listener.ReconciliationSkipListener;
import com.fintech.reconcile.batch.processor.ReconciliationItemProcessor;
import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.service.FileLifecycleService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Timestamp;

@Configuration
public class BatchConfig {

  @Value("${app.reconciliation.chunk-size:500}")
  private int chunkSize;

  // 1. Reader
  @Bean
  @StepScope
  public FlatFileItemReader<ThirdPartyMutationRecord> csvMutationReader(
      @Value("#{jobParameters['filePath']}") String filePath,
      FileLifecycleService fileLifecycleService) {

    File targetFile;

    if (filePath != null && !filePath.isBlank()) {
      targetFile = new File(filePath);
    } else {
      targetFile = fileLifecycleService.getNextAvailableFile()
          .orElseThrow(() -> new IllegalStateException(
              "[BATCH] Tidak ada file .csv apa pun di direktori incoming: "
                  + fileLifecycleService.getIncomingPath()));
    }

    return new FlatFileItemReaderBuilder<ThirdPartyMutationRecord>()
        .name("csvMutationReader")
        .resource(new FileSystemResource(targetFile))
        .delimited()
        .names("referenceId", "accountNumber", "amount", "transactionType", "transactionTime")
        .linesToSkip(1) // Lewati header CSV
        .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
          {
            setTargetType(ThirdPartyMutationRecord.class);
          }
        })
        .build();
  }

  // 2. WRITER: Native JDBC Batching
  @Bean
  public JdbcBatchItemWriter<ReconciliationResult> reconciliationResultWriter(DataSource dataSource) {
    String sql = """
            INSERT INTO reconciliation_results
            (job_execution_id, reference_id, internal_amount, third_party_amount, amount_discrepancy, status, discrepancy_reason, reconciled_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

    return new JdbcBatchItemWriterBuilder<ReconciliationResult>()
        .dataSource(dataSource)
        .sql(sql)
        .itemPreparedStatementSetter((item, ps) -> {
          ps.setLong(1, item.getJobExecutionId());
          ps.setString(2, item.getReferenceId());
          ps.setBigDecimal(3, item.getInternalAmount());
          ps.setBigDecimal(4, item.getThirdPartyAmount());
          ps.setBigDecimal(5, item.getAmountDiscrepancy());
          ps.setString(6, item.getStatus().name());
          ps.setString(7, item.getDiscrepancyReason());
          ps.setTimestamp(8, Timestamp.valueOf(item.getReconciledAt()));
        })
        .build();
  }

  // 3. STEP: Reader -> Processor -> Writer + Fault Tolerance
  @Bean
  public Step reconciliationStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      FlatFileItemReader<ThirdPartyMutationRecord> reader,
      ReconciliationItemProcessor processor,
      ItemWriter<ReconciliationResult> writer,
      ReconciliationSkipListener skipListener) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<ThirdPartyMutationRecord, ReconciliationResult>chunk(chunkSize, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skip(Exception.class)
        .skipLimit(25)
        .listener(skipListener)
        .build();
  }

  // 4. JOB: Pipeline Entry Point
  @Bean
  public Job paymentReconciliationJob(JobRepository jobRepository,
      Step reconciliationStep,
      ReconciliationJobExecutionListener auditListener) {
    return new JobBuilder("paymentReconciliationJob", jobRepository)
        .start(reconciliationStep)
        .listener(auditListener)
        .build();
  }
}