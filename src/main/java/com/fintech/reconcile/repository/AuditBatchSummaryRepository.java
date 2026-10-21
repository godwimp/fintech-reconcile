package com.fintech.reconcile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintech.reconcile.domain.entity.AuditBatchSummary;

@Repository
public interface AuditBatchSummaryRepository extends JpaRepository<AuditBatchSummary, Long> {

}
