package com.restaurant.backend.service;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.Customer;

import java.util.List;

public interface CustomerService {
    Customer create(Customer customer);
    List<Customer> getAll(ListScope scope);
    Customer getById(Long id);
    Customer update(Long id, Customer customer);
    void delete(Long id);        // chuyển vào thùng rác
    Customer restore(Long id);   // khôi phục từ thùng rác
    void purge(Long id);         // xóa vĩnh viễn
}
