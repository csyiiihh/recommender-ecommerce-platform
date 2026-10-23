package org.example.springboot.dto;

/** Counts across all orders owned by the authenticated buyer. */
public record OrderSummary(long total, long unpaid, long pending) { }
