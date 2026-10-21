package com.fintech.reconcile.controller;

import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reconciliation")
public class ReconciliationTriggerController {
  private final JobLauncher jobLauncher;
  private final Job paymentReconciliationJob;

  public ReconciliationTriggerController(JobLauncher jobLauncher, Job paymentReconciliationJob) {
    this.jobLauncher = jobLauncher;
    this.paymentReconciliationJob = paymentReconciliationJob;
  }

  @PostMapping("/run")
  public ResponseEntity<Map<String, Object>> triggerReconciliation() throws Exception {
    JobParameters jobParameters = new JobParametersBuilder()
        .addLong("executionTime", System.currentTimeMillis())
        .toJobParameters();
    
    var execution = jobLauncher.run(paymentReconciliationJob, jobParameters);

    return ResponseEntity.ok(Map.of(
      "jobExecutionid", execution.getId(),
      "status", execution.getStatus().toString(),
      "startTime", execution.getStartTime() != null ? execution.getStartTime().toString() : "N/A"
    ));
  }
}
