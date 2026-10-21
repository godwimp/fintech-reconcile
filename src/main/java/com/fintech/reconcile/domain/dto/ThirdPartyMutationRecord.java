package com.fintech.reconcile.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThirdPartyMutationRecord {
  private String referenceId;
  private String accountNumber;
  private BigDecimal amount;
  private String transactionType;
  private String transactionTime;
}
