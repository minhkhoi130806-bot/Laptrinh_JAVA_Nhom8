package com.restaurant.backend.repository;

import com.restaurant.backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    // Lọc theo thùng rác: false = đang dùng, true = đã xóa mềm
    List<Customer> findByDeleted(Boolean deleted);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
