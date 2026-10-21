package com.fintech.reconcile.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintech.reconcile.domain.entity.InternalTransaction;

@Repository 
public interface InternalTransactionRepository extends JpaRepository<InternalTransaction, Long> {
  Optional<InternalTransaction> findByReferenceId(String referenceId);
}
