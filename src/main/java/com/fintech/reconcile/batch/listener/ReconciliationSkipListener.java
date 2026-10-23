package com.fintech.reconcile.batch.listener;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.QuarantinedRecord;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.repository.QuarantinedRecordRepository;

import io.micrometer.common.lang.NonNull;

@Component
public class ReconciliationSkipListener {
  private static final Logger log = LoggerFactory.getLogger(ReconciliationSkipListener.class);
  private final QuarantinedRecordRepository quarantineRepository;
  private Long jobExecutionId;

  public ReconciliationSkipListener(QuarantinedRecordRepository quarantineRepository) {
    this.quarantineRepository = quarantineRepository;
  }

  @BeforeStep
  public void BeforeStep(StepExecution stepExecution) {
    this.jobExecutionId = stepExecution.getJobExecution().getId();
  }

  // @Override
  public void onSkipInRead(@NonNull Throwable t) {
    int lineNumber = -1;
    String rawInput = "UNKNOWN";

    if (t instanceof FlatFileParseException parseEx) {
      lineNumber = parseEx.getLineNumber();
      rawInput = parseEx.getInput();
    }

    log.error(">>> [QUARANTINE - READ ERROR] Line {}: Corrupted record detected. Input: '{}'. Reason: {}",
        lineNumber, rawInput, t.getMessage());

    QuarantinedRecord record = QuarantinedRecord.builder()
        .jobExecutionId(jobExecutionId)
        .lineNumber(lineNumber)
        .rawPayload(rawInput)
        .errorType(t.getClass().getSimpleName())
        .errorMessage(t.getMessage())
        .quarantinedAt(LocalDateTime.now())
        .build();

    quarantineRepository.save(record);
  }

  public void onSkipInProcess(@NonNull ThirdPartyMutationRecord item, @NonNull Throwable t) {
    log.error(">>> [QUARANTINE- PROCESS ERROR] Failed to process record RefId: {}. Reason: {}", item.getReferenceId(),
        t.getMessage());

    QuarantinedRecord record = QuarantinedRecord.builder()
        .jobExecutionId(jobExecutionId)
        .rawPayload(item.toString())
        .errorType(t.getClass().getSimpleName())
        .errorMessage(t.getMessage())
        .quarantinedAt(LocalDateTime.now())
        .build();

    quarantineRepository.save(record);
  }

  public void onSkipInWrite(@NonNull ReconciliationResult item, @NonNull Throwable t) {
    log.error(">>> [QUARANTINE - WRITE ERROR] Failed to write result RefId: {}. Reason: {}", item.getReferenceId(),
        t.getMessage());

    QuarantinedRecord record = QuarantinedRecord.builder()
        .jobExecutionId(jobExecutionId)
        .rawPayload(item.toString())
        .errorType(t.getClass().getSimpleName())
        .errorMessage(t.getMessage())
        .quarantinedAt(LocalDateTime.now())
        .build();

    quarantineRepository.save(record);
  }

}
