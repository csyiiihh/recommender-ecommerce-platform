package org.example.springboot.dto;
import java.sql.Timestamp;
public record PublicReview(Long id, Long productId, Integer rating, String content, Timestamp createdAt, Reviewer user) {
    public record Reviewer(String username, String avatar) { }
}
