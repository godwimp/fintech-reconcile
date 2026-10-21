package com.fintech.reconcile.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.InternalTransaction;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.domain.enums.ReconStatus;
import com.fintech.reconcile.repository.InternalTransactionRepository;

import io.micrometer.common.lang.NonNull;

@Component 
public class ReconciliationItemProcessor implements ItemProcessor<ThirdPartyMutationRecord, ReconciliationResult> {
  private static final Logger log = LoggerFactory.getLogger(ReconciliationItemProcessor.class);
  private final InternalTransactionRepository internalTxRepository;
  private Long jobExecutionId;

  public ReconciliationItemProcessor(InternalTransactionRepository internalTxRepository) {
    this.internalTxRepository = internalTxRepository;
  }

  @BeforeStep 
  public void beforeStep(StepExecution stepExecution) {
    this.jobExecutionId = stepExecution.getJobExecution().getId();
  }

  @Override 
  public ReconciliationResult process(@NonNull ThirdPartyMutationRecord thirdPartyRecord) {
    String refId = thirdPartyRecord.getReferenceId();
    BigDecimal thirdPartyAmount = thirdPartyRecord.getAmount();
    Optional<InternalTransaction> internalTxOpt = internalTxRepository.findByReferenceId(refId);

    // UNMATCHED
    if (internalTxOpt.isEmpty()) {
      log.warn("UNMATCHED: ReferenceId {} not found in internal records", refId);
      return ReconciliationResult.builder()
          .jobExecutionId(jobExecutionId)
          .referenceId(refId)
          .internalAmount(BigDecimal.ZERO)
          .thirdPartyAmount(thirdPartyAmount)
          .amountDiscrepancy(thirdPartyAmount)
          .status(ReconStatus.UNMATCHED)
          .discrepancyReason("Record exists only in Third Party Statement")
          .reconciledAt(LocalDateTime.now())
          .build();
    }

    InternalTransaction internalTx = internalTxOpt.get();
    BigDecimal internalAmount = internalTx.getAmount();

    // SUSPECT
    if (internalAmount.compareTo(thirdPartyAmount) != 0) {
      BigDecimal discrepancy = internalAmount.subtract(thirdPartyAmount).abs();
      log.warn("SUSPECT: ReferenceId {} has amount discrepancy. Internal: {}, Third Party: {}", refId, internalAmount, thirdPartyAmount);
      return ReconciliationResult.builder()
          .jobExecutionId(jobExecutionId)
          .referenceId(refId)
          .internalAmount(internalAmount)
          .thirdPartyAmount(thirdPartyAmount)
          .amountDiscrepancy(discrepancy)
          .status(ReconStatus.SUSPECT)
          .discrepancyReason("Amount discrepancy detected.")
          .reconciledAt(LocalDateTime.now())
          .build();
    }

    // MATCHED
    return ReconciliationResult.builder()
        .jobExecutionId(jobExecutionId)
        .referenceId(refId)
        .internalAmount(internalAmount)
        .thirdPartyAmount(thirdPartyAmount)
        .amountDiscrepancy(BigDecimal.ZERO)
        .status(ReconStatus.MATCHED)
        .discrepancyReason("Exact match")
        .reconciledAt(LocalDateTime.now())
        .build();
  }
}
