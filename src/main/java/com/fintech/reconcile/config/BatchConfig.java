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
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Timestamp;

@Configuration
public class BatchConfig {

  @Value("${app.reconciliation.chunk-size:500}")
  private int chunkSize;

  @Value("${app.reconciliation.thread-pool-size:4}")
  private int threadPoolSize;

  // 1. Thread Pool Task Executor
  @Bean
  public TaskExecutor reconciliationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(threadPoolSize);
    executor.setMaxPoolSize(threadPoolSize * 2);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("recon-worker-");
    executor.initialize();
    return executor;
  }

  // 2. READER: Unsynchronized FlatFileItemReader
  public FlatFileItemReader<ThirdPartyMutationRecord> rawCsvMutationReader(File targetFile) {
    return new FlatFileItemReaderBuilder<ThirdPartyMutationRecord>()
        .name("rawCsvMutationReader")
        .resource(new FileSystemResource(targetFile))
        .delimited()
        .names("referenceId", "accountNumber", "amount", "transactionType", "transactionTime")
        .linesToSkip(1)
        .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
          {
            setTargetType(ThirdPartyMutationRecord.class);
          }
        })
        .build();
  }

  // 3. Sync Reader
  @Bean
  @StepScope
  public SynchronizedItemStreamReader<ThirdPartyMutationRecord> csvMutationReader(
      @Value("#{jobParameters['filePath']}") String filePath,
      FileLifecycleService fileLifecycleService) {
    File targetFile;
    if (filePath != null && !filePath.isBlank()) {
      targetFile = new File(filePath);
    } else {
      targetFile = fileLifecycleService.getNextAvailableFile()
          .orElseThrow(() -> new IllegalStateException(
              "[BATCH] Tidak ada file .csv di direktori incoming: " + fileLifecycleService.getIncomingPath()));
    }
    FlatFileItemReader<ThirdPartyMutationRecord> delegate = rawCsvMutationReader(targetFile);
    return new SynchronizedItemStreamReaderBuilder<ThirdPartyMutationRecord>()
        .delegate(delegate)
        .build();
  }

  // 4. WRITER
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

  // 5. Multi-threaded Step
  @Bean
  public Step reconciliationStep(JobRepository jobRepository,
      PlatformTransactionManager transactionManager,
      SynchronizedItemStreamReader<ThirdPartyMutationRecord> reader,
      ReconciliationItemProcessor processor,
      ItemWriter<ReconciliationResult> writer,
      ReconciliationSkipListener skipListener,
      TaskExecutor reconciliationTaskExecutor) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<ThirdPartyMutationRecord, ReconciliationResult>chunk(chunkSize, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skip(Exception.class)
        .skipLimit(25)
        .listener(skipListener)
        .taskExecutor(reconciliationTaskExecutor)
        .throttleLimit(threadPoolSize)
        .build();
  }

  // 6. Job
  @Bean
  public Job paymentReconciliationJob(JobRepository jobRepository, Step reconciliationStep,
      ReconciliationJobExecutionListener auditListener) {
    return new JobBuilder("paymentReconciliationJob", jobRepository)
        .start(reconciliationStep)
        .listener(auditListener)
        .build();
  }
}