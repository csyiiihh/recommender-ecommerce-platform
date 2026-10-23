package org.example.springboot.dto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
public record PublicReviewPage(List<PublicReview> records, long total, long current, long size, long pages, Summary summary) {
    public record Summary(long total, BigDecimal average, Map<Integer, Long> distribution) {}
}
