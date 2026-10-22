package com.fintech.reconcile.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.fintech.reconcile.batch.listener.ReconciliationJobExecutionListener;
import com.fintech.reconcile.batch.processor.ReconciliationItemProcessor;
import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.repository.ReconciliationResultRepository;

@Configuration
public class BatchConfig {
  @Value("${app.reconciliation.incoming-dir:data/incoming}")
  private String inputFilePath;

  @Value("${app.reconciliation.chunk-size:500}")
  private int chunkSize;

  // READER
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
        .names("referenceId", "accountNumber", "amount", "transactionType", "transactionTime")
        .linesToSkip(1)
        .fieldSetMapper(new BeanWrapperFieldSetMapper<>() {
          {
            setTargetType(ThirdPartyMutationRecord.class);
          }
        })
        .build();
  }

  // WRITER
  @Bean
  public ItemWriter<ReconciliationResult> reconciliationResultWriter(ReconciliationResultRepository repository) {
    return repository::saveAll;
  }

  // COMBINING
  @Bean
  public Step reconciliationStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
      FlatFileItemReader<ThirdPartyMutationRecord> reader, ReconciliationItemProcessor processor,
      ItemWriter<ReconciliationResult> writer) {
    return new StepBuilder("reconciliationStep", jobRepository)
        .<ThirdPartyMutationRecord, ReconciliationResult>chunk(chunkSize, transactionManager)
        .reader(reader)
        .processor(processor)
        .writer(writer)
        .faultTolerant()
        .skip(Exception.class)
        .skipLimit(10)
        .build();
  }

  // ENTRY POINT
  @Bean
  public Job paymentReconciliationJob(JobRepository jobRepository, Step reconciliationStep,
      ReconciliationJobExecutionListener auditListener) {
    return new JobBuilder("paymentReconciliationJob", jobRepository)
        .start(reconciliationStep)
        .listener(auditListener)
        .build();
  }
}
