package com.fintech.reconcile.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_batch_summaries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditBatchSummary {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_execution_id", nullable = false, unique = true)
  private Long jobExecutionId;

  @Column(name = "total_records_processed", nullable = false)
  private Integer totalRecordsProcessed;

  @Column(name = "total_matched_count", nullable = false)
  private Integer totalMatchedCount;

  @Column(name = "total_suspect_count", nullable = false)
  private Integer totalSuspectCount;

  @Column(name = "total_unmatched_count", nullable = false)
  private Integer totalUnmatchedCount;

  @Column(name = "total_suspect_amount", precision = 15, scale = 2)
  private BigDecimal totalSuspectAmount;

  @Column(name = "match_rate_percentage", precision = 5, scale = 2)
  private BigDecimal matchRatePercentage;

  @Column(name = "duration_millis", nullable = false)
  private Long durationMillis;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "completed_at", nullable = false)
  private LocalDateTime completedAt;
}
