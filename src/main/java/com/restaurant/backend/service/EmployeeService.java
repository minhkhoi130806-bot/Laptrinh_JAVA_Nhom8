package com.restaurant.backend.service;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.Employee;

import java.util.List;

public interface EmployeeService {
    Employee create(Employee employee);
    List<Employee> getAll(ListScope scope);
    Employee getById(Long id);
    Employee update(Long id, Employee employee);
    void delete(Long id);        // chuyển vào thùng rác
    Employee restore(Long id);   // khôi phục từ thùng rác
    void purge(Long id);         // xóa vĩnh viễn
}
