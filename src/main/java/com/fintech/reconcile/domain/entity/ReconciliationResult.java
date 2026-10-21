package com.fintech.reconcile.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fintech.reconcile.domain.enums.ReconStatus;

@Entity 
@Table (name = "reconciliation_results")
@Getter
@Setter
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class ReconciliationResult {
  
  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private Long id;

  @Column (name = "job_execution_id", nullable = false)
  private Long jobExecutionId;

  @Column (name = "reference_id", nullable = false, length = 64)
  private String referenceId;

  @Column (name = "internal_amount", precision = 15, scale = 2)
  private BigDecimal internalAmount;

  @Column (name = "third_party_amount", precision = 15, scale = 2)
  private BigDecimal thirdPartyAmount;

  @Column (name = "amount_discrepancy", precision = 15, scale = 2)
  private BigDecimal amountDiscrepancy;

  @Enumerated (EnumType.STRING)
  @Column (name = "status", nullable = false, length = 20)
  private ReconStatus status;

  @Column (name = "discrepancyReason")
  private String discrepancyReason;

  @Column (name = "reconciled_at", nullable = false)
  private LocalDateTime reconciledAt;
}
