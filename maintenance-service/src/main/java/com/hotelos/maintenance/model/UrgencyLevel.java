package com.hotelos.maintenance.model;

/**
 * Urgency levels for the priority queue algorithm.
 * Ordinal value used for comparison: CRITICAL(0) > HIGH(1) > NORMAL(2) > LOW(3).
 * Lower ordinal = higher priority.
 */
public enum UrgencyLevel {
    CRITICAL,   // ordinal 0 — highest priority
    HIGH,       // ordinal 1
    NORMAL,     // ordinal 2
    LOW         // ordinal 3 — lowest priority
}
