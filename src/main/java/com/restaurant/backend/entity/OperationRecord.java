package com.restaurant.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "operation_record", indexes = @Index(name = "idx_operation_type_created", columnList = "record_type,created_at"))
@Getter
@Setter
public class OperationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "record_type", nullable = false, length = 32)
    private String recordType;
    @Column(name = "singleton_key", unique = true, length = 50)
    private String singletonKey;
    @Column(name = "fields_json", nullable = false, columnDefinition = "TEXT")
    private String fieldsJson;
    @Column(nullable = false)
    private boolean done;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }
}
