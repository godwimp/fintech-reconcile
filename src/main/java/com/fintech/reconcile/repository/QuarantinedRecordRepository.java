package com.fintech.reconcile.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fintech.reconcile.domain.entity.QuarantinedRecord;

@Repository
public interface QuarantinedRecordRepository extends JpaRepository<QuarantinedRecord, Long> {

}
