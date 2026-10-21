package com.fintech.reconcile.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity 
@Table(name = "internal_transactions")
@Getter
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder 
public class InternalTransaction {

  @Id 
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private Long id;

  @Column (name = "reference_id", nullable = false, unique = true, length = 64)
  private String referenceId;

  @Column (name = "account_number", nullable = false, length = 32)
  private String accountNumber;

  @Column (name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  @Column (name = "transaction_type", nullable = false, length = 10)
  private String transactionType;

  @Column (name = "transaction_time", nullable = false)
  private LocalDateTime transactionTime;

  @Column (name = "status", nullable = false, length = 20)
  private String status;
}