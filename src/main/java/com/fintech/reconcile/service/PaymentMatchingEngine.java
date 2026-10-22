package com.fintech.reconcile.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.fintech.reconcile.domain.dto.ThirdPartyMutationRecord;
import com.fintech.reconcile.domain.entity.InternalTransaction;
import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.domain.enums.DiscrepancyReason;
import com.fintech.reconcile.domain.enums.ReconStatus;

@Service
public class PaymentMatchingEngine {
  // Eval the third party data and internal data.
  public ReconciliationResult match(Long jobExecutionId, ThirdPartyMutationRecord thirdPartyRecord,
      Optional<InternalTransaction> internalTxOpt) {
    String refId = thirdPartyRecord.getReferenceId();
    BigDecimal thirdPartyAmount = thirdPartyRecord.getAmount();

    // 1. UNMATCHED
    if (internalTxOpt.isEmpty()) {
      return buildResult(jobExecutionId, refId, BigDecimal.ZERO, thirdPartyAmount, thirdPartyAmount,
          ReconStatus.UNMATCHED, DiscrepancyReason.THIRD_PARTY_ONLY);
    }

    InternalTransaction internalTx = internalTxOpt.get();
    BigDecimal internalAmount = internalTx.getAmount();

    // 2. SUSPECT
    if (internalAmount.compareTo(thirdPartyAmount) != 0) {
      BigDecimal discrepancy = internalAmount.subtract(thirdPartyAmount).abs();
      return buildResult(jobExecutionId, refId, internalAmount, thirdPartyAmount, discrepancy, ReconStatus.SUSPECT,
          DiscrepancyReason.AMOUNT_MISMATCH);
    }

    // 3. MATCHED
    return buildResult(jobExecutionId, refId, internalAmount, thirdPartyAmount, BigDecimal.ZERO, ReconStatus.MATCHED,
        DiscrepancyReason.EXACT_MATCH);
  }

  private ReconciliationResult buildResult(Long jobExecutionId, String referenceId, BigDecimal internalAmount,
      BigDecimal thirdPartyAmount, BigDecimal discrepancy, ReconStatus status, DiscrepancyReason reason) {
    return ReconciliationResult.builder()
        .jobExecutionId(jobExecutionId)
        .referenceId(referenceId)
        .internalAmount(internalAmount)
        .thirdPartyAmount(thirdPartyAmount)
        .amountDiscrepancy(discrepancy)
        .status(status)
        .discrepancyReason(reason.getDescription())
        .reconciledAt(LocalDateTime.now())
        .build();
  }
}
