package com.tkiet.qms.entity;

public enum TokenStatus {
    WAITING,    // student booked, in queue
    SERVING,    // currently at counter
    DONE,       // completed
    SKIPPED,    // admin skipped them
    CANCELLED   // student cancelled
}