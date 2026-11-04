package com.restaurant.backend.repository;

import com.restaurant.backend.entity.DiningTable;
import com.restaurant.backend.entity.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from DiningTable t where t.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") Long id);

    // Lọc theo thùng rác: false = đang dùng, true = đã xóa mềm
    List<DiningTable> findByDeleted(Boolean deleted);

    boolean existsByTableCode(String tableCode);

    boolean existsByTableCodeAndIdNot(String tableCode, Long id);

    List<DiningTable> findByStatus(TableStatus status);
}
