package com.fintech.reconcile.scheduler;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fintech.reconcile.service.FileLifecycleService;

@Component
@ConditionalOnProperty(name = "app.reconciliation.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ReconciliationScheduler {
  private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduler.class);

  private final JobLauncher jobLauncher;
  private final Job paymentReconciliationJob;
  private final FileLifecycleService fileLifecycleService;

  public ReconciliationScheduler(JobLauncher jobLauncher, Job paymentReconciliationJob,
      FileLifecycleService fileLifecycleService) {
    this.jobLauncher = jobLauncher;
    this.paymentReconciliationJob = paymentReconciliationJob;
    this.fileLifecycleService = fileLifecycleService;
  }

  @Scheduled(cron = "${app.reconciliation.cron}")
  public void runScheduledReconciliation() {
    Optional<File> incomingFileOpt = fileLifecycleService.getNextAvailableFile();
    if (incomingFileOpt.isEmpty()) {
      log.info("[CRON] No incoming file found. Skipping.");
      return;
    }

    File fileToProcess = incomingFileOpt.get();
    log.info("[CRON] Found incoming file: {}. Triggering reconciliation job.", fileToProcess.getName());

    try {
      JobParameters jobParameters = new JobParametersBuilder()
          .addString("filePath", fileToProcess.getAbsolutePath())
          .addLong("timestamp", System.currentTimeMillis())
          .toJobParameters();

      JobExecution execution = jobLauncher.run(paymentReconciliationJob, jobParameters);

      if (execution.getStatus() == BatchStatus.COMPLETED) {
        log.info("[CRON] Reconciliation job completed. Archiving file...");
        fileLifecycleService.archiveFile(fileToProcess);
      } else {
        log.error("[CRON] Reconciliation job ended with status: {}. File NOT archived.", execution.getStatus());
      }
    } catch (Exception e) {
      log.error("[CRON] Fatal error during scheduled reconciliation execution", e);
    }
  }
}
