package com.fintech.reconcile.config;

import java.sql.Timestamp;

import javax.sql.DataSource;

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

import com.fintech.reconcile.batch.listener.ReconciliationJobExecutionListener;
import com.fintech.reconcile.batch.listener.ReconciliationSkipListener;
import com.fintech.reconcile.batch.processor.ReconciliationItemProcessor;
import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.ReconciliationResult;

@Configuration
public class BatchConfig {
  @Value("${app.reconciliation.chunk-size:500}")
  private int chunkSize;

  // 1. Reader
  @Bean
  @StepScope
  public FlatFileItemReader<ThirdPartyMutationRecord> csvMutationReader(
      @Value("#{jobParameters['filePath']}") String filePath) {
    if (filePath == null) {
      filePath = "data/incoming/mutation.csv";
    }

    return new FlatFileItemReaderBuilder<ThirdPartyMutationRecord>()
        .name("csvMutationReader")
        .resource(new FileSystemResource(filePath))
        .delimited()
        .names("referenceId", "amount", "transactionType", "transactionTime")
        .linesToSkip(1)
        .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
          {
            setTargetType(ThirdPartyMutationRecord.class);
          }
        })
        .build();
  }

  // 2. Writer using Native JDBC Batching
  @Bean
  public JdbcBatchItemWriter<ReconciliationResult> reconciliationResultWriter(DataSource dataSource) {
    String sql = """
        INSERT INTO reconciliation_results (job_execution_id, reference_id, internal_amount, third_party_amount, amount_discrepancy, status, discrepancy_reason, reconciled_at) VALUES(?,?,?,?,?,?,?,?)
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

  // 3. Reader -> Processor -> Writer
  @Bean
  public Step reconciliationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      FlatFileItemReader<ThirdPartyMutationRecord> reader, ReconciliationItemProcessor processor,
      ItemWriter<ReconciliationResult> writer, ReconciliationSkipListener skipListener) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<ThirdPartyMutationRecord, ReconciliationResult>chunk(chunkSize, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skipLimit(25)
        .skip(Exception.class)
        .listener(skipListener)
        .build();
  }

  // 4. Pipeline Entry Point
  @Bean
  public Job paymentReconciliationJob(JobRepository jobRepository, Step reconciliationStep,
      ReconciliationJobExecutionListener auditListener) {
    return new JobBuilder("paymentReconciliationJob", jobRepository)
        .listener(auditListener)
        .start(reconciliationStep)
        .build();
  }
}
