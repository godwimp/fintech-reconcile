package com.fintech.reconcile.domain.enums;

public enum DiscrepancyReason {
  EXACT_MATCH("Exact match on reference and settlement amount"),
  AMOUNT_MISMATCH("Amount discrepancy detected between internal ledger and third party statement"),
  THIRD_PARTY_ONLY("Record exists only in Third-Party statement (Potential orphan settlement)"),
  INTERNAL_ONLY("Record exists only in Internal database (Pending settlement)");

  private final String description;

  DiscrepancyReason(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
