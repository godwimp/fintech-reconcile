package com.fintech.reconcile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintech.reconcile.domain.entity.ReconciliationResult;

@Repository 
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
  
}