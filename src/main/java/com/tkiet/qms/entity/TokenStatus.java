package com.tkiet.qms.entity;

public enum TokenStatus {
    PENDING,    // student applied, waiting for admin approval
    APPROVED,   // admin approved, certificate sent
    REJECTED,   // admin rejected
    CANCELLED ,  // student cancelled
    DONE
}
