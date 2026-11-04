package com.restaurant.backend.repository;

import com.restaurant.backend.entity.OperationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OperationRecordRepository extends JpaRepository<OperationRecord, Long> {
    List<OperationRecord> findByRecordTypeOrderByCreatedAtDescIdDesc(String recordType);
    Optional<OperationRecord> findBySingletonKey(String singletonKey);
}
