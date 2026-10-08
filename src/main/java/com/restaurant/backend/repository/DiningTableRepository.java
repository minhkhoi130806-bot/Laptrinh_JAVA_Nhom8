package com.restaurant.backend.repository;

import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    boolean existsByTableCode(String tableCode);

    boolean existsByTableCodeAndIdNot(String tableCode, Long id);

    List<DiningTable> findByStatus(TableStatus status);
}
