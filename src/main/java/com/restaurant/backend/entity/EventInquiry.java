package com.restaurant.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "event_inquiry")
@Getter @Setter
public class EventInquiry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "contact_name", nullable = false, length = 100) private String contactName;
    @Column(nullable = false, length = 25) private String phone;
    @Column(length = 190) private String email;
    @Column(name = "event_type", nullable = false, length = 60) private String eventType;
    @Column(name = "event_at") private LocalDateTime eventAt;
    @Column(name = "guest_count", nullable = false) private Integer guestCount;
    @Column(length = 60) private String budget;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false, length = 24) private String status = "NEW";
    @Column(name = "quote_amount", precision = 14, scale = 2) private java.math.BigDecimal quoteAmount;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
