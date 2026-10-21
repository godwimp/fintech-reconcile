package com.fintech.reconcile.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.fintech.reconcile.domain.entity.ReconciliationResult;
import com.fintech.reconcile.domain.enums.ReconStatus;

@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
  long countByJobExecutionId(Long jobExecutionId);

  long countByJobExecutionIdAndStatus(Long jobExecutionId, ReconStatus status);

  @Query("SELECT COALESCE(SUM(r.amountDiscrepancy), 0) FROM ReconciliationResult r "
      + "WHERE r.jobExecutionId = :jobExecutionId AND r.status = :status")
  BigDecimal sumAmountDiscrepancyByJobExecutionIdAndStatus(@Param("jobExecutionId") Long jobExecutionId,
      @Param("status") ReconStatus status);

  List<ReconciliationResult> findByJobExecutionId(Long jobExecutionId);
}