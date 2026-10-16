package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Lọc theo thùng rác: false = đang dùng, true = đã xóa mềm
    List<Employee> findByDeleted(Boolean deleted);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);
}
