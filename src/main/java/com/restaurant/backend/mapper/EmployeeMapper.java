package com.restaurant.backend.mapper;

import com.restaurant.backend.dto.EmployeeRequest;
import com.restaurant.backend.dto.EmployeeResponse;
import com.restaurant.backend.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class EmployeeMapper {

    public Employee toEntity(EmployeeRequest request) {
        if (request == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setUsername(request.username());
        employee.setFullName(request.fullName());
        employee.setPassword(request.password());
        employee.setRole(request.role());
        employee.setPhone(request.phone());
        employee.setSalary(request.salary());
        return employee;
    }

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getUsername(),
                employee.getFullName(),
                employee.getRole(),
                employee.getPhone(),
                employee.getSalary()
        );
    }
}
