package com.fintech.reconcile.batch.listener;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import com.fintech.reconcile.domain.entity.AuditBatchSummary;
import com.fintech.reconcile.repository.AuditBatchSummaryRepository;
import com.fintech.reconcile.repository.ReconciliationResultRepository;

@Component
public class ReconciliationJobExecutionListener implements JobExecutionListener {
  private static final Logger log = LoggerFactory.getLogger(ReconciliationJobExecutionListener.class);
  private final ReconciliationResultRepository resultRepository;
  private final AuditBatchSummaryRepository auditRepository;

  public ReconciliationJobExecutionListener(ReconciliationResultRepository resultRepository,
      AuditBatchSummaryRepository auditRepository) {
    this.resultRepository = resultRepository;
    this.auditRepository = auditRepository;
  }

  @Override
  public void beforeJob(JobExecution jobExecution) {
    log.info("==========");
    log.info("Starting Reconciliation Job [Execution ID: {}] at {}", jobExecution.getId(), jobExecution.getStartTime());
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    Long jobId = jobExecution.getId();
    Duration duration = Duration.between(jobExecution.getStartTime(),
        jobExecution.getEndTime() != null ? jobExecution.getEndTime() : LocalDateTime.now());
    long durationMillis = duration.toMillis();
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
      long totalCount = resultRepository.countByJobExecutionId(jobId);
      long matchedCount = resultRepository.countByJobExecutionIdAndStatus(jobId,
          com.fintech.reconcile.domain.enums.ReconStatus.MATCHED);
      long suspectCount = resultRepository.countByJobExecutionIdAndStatus(jobId,
          com.fintech.reconcile.domain.enums.ReconStatus.SUSPECT);
      long unmatchedCount = resultRepository.countByJobExecutionIdAndStatus(jobId,
          com.fintech.reconcile.domain.enums.ReconStatus.UNMATCHED);
      BigDecimal totalSuspectAmount = resultRepository.sumAmountDiscrepancyByJobExecutionIdAndStatus(jobId,
          com.fintech.reconcile.domain.enums.ReconStatus.SUSPECT);

      BigDecimal matchRate = (totalCount > 0)
          ? BigDecimal.valueOf(matchedCount)
              .multiply(BigDecimal.valueOf(100))
              .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP)
          : BigDecimal.ZERO;

      AuditBatchSummary summary = AuditBatchSummary.builder()
          .jobExecutionId(jobId)
          .totalRecordsProcessed((int) totalCount)
          .totalMatchedCount((int) matchedCount)
          .totalSuspectCount((int) suspectCount)
          .totalUnmatchedCount((int) unmatchedCount)
          .totalSuspectAmount(totalSuspectAmount)
          .matchRatePercentage(matchRate)
          .durationMillis(durationMillis)
          .status(jobExecution.getStatus().name())
          .completedAt(LocalDateTime.now())
          .build();

      auditRepository.save(summary);
      log.info("==================================================================");
      log.info("               EXECUTIVE RECONCILIATION SUMMARY                   ");
      log.info("==================================================================");
      log.info(" Job Execution ID       : {}", jobId);
      log.info(" Status                 : {}", jobExecution.getStatus());
      log.info(" Duration               : {} ms (~{} s)", durationMillis, duration.toSeconds());
      log.info(" Total Ingested Records : {}", totalCount);
      log.info(" MATCHED Records        : {} ({}%)", matchedCount, matchRate);
      log.info(" SUSPECT Records        : {}", suspectCount);
      log.info(" UNMATCHED Records      : {}", unmatchedCount);
      log.info(" Total Discrepancy (IDR): Rp {}", totalSuspectAmount);
      log.info("==================================================================");
    } else {
      log.error(">>> RECONCILIATION JOB FAILED [Execution ID: {}] with status: {}",
          jobId, jobExecution.getStatus());
    }
  }
}
