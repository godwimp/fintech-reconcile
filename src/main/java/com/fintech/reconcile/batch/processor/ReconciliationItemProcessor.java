package com.fintech.reconcile.batch.processor;

import java.util.Optional;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.InternalTransaction;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.repository.InternalTransactionRepository;
import com.fintech.reconcile.service.PaymentMatchingEngine;

@Component
public class ReconciliationItemProcessor implements ItemProcessor<ThirdPartyMutationRecord, ReconciliationResult> {
  private final InternalTransactionRepository internalTxRepository;
  private final PaymentMatchingEngine matchingEngine;
  private Long jobExecutionId;

  public ReconciliationItemProcessor(InternalTransactionRepository internalTxRepository,
      PaymentMatchingEngine matchingEngine) {
    this.internalTxRepository = internalTxRepository;
    this.matchingEngine = matchingEngine;
  }

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    this.jobExecutionId = stepExecution.getJobExecution().getId();
  }

  @Override
  public ReconciliationResult process(@NonNull ThirdPartyMutationRecord thirdPartyRecord) {
    Optional<InternalTransaction> internalTxOpt = internalTxRepository
        .findByReferenceId(thirdPartyRecord.getReferenceId());

    return matchingEngine.match(jobExecutionId, thirdPartyRecord, internalTxOpt);
  }
}
