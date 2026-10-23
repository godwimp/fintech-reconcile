package com.fintech.reconcile.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "quarantined_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuarantinedRecord {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_execution_id", nullable = false)
  private Long jobExecutionId;

  @Column(name = "line_number")
  private Integer lineNumber;

  @Column(name = "raw_payload", columnDefinition = "TEXT")
  private String rawPayload;

  @Column(name = "error_type", nullable = false, length = 100)
  private String errorType;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "quarantined_at", nullable = false)
  private LocalDateTime quarantinedAt;
}