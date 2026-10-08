package com.restaurant.backend.service.Impl;

import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.service.EmployeeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public Employee create(Employee employee) {
        validate(employee);
        if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            throw new BadRequestException("Mật khẩu không được để trống");
        }
        if (employeeRepository.existsByUsername(employee.getUsername())) {
            throw new BadRequestException("Username đã tồn tại");
        }
        employee.setId(null);
        // TODO: mã hóa mật khẩu (BCrypt) khi nhóm thêm Spring Security
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên id = " + id));
    }

    @Override
    @Transactional
    public Employee update(Long id, Employee data) {
        validate(data);
        Employee existing = getById(id);
        if (employeeRepository.existsByUsernameAndIdNot(data.getUsername(), id)) {
            throw new BadRequestException("Username đã tồn tại");
        }
        existing.setUsername(data.getUsername());
        existing.setFullName(data.getFullName());
        existing.setRole(data.getRole());
        existing.setPhone(data.getPhone());
        existing.setSalary(data.getSalary());
        // Chỉ đổi mật khẩu khi request có gửi password mới
        if (data.getPassword() != null && !data.getPassword().isBlank()) {
            existing.setPassword(data.getPassword());
        }
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(getById(id));
    }

    private void validate(Employee e) {
        if (e == null || e.getUsername() == null || e.getUsername().isBlank()) {
            throw new BadRequestException("Username không được để trống");
        }
        if (e.getFullName() == null || e.getFullName().isBlank()) {
            throw new BadRequestException("Họ tên nhân viên không được để trống");
        }
        if (e.getRole() == null) {
            throw new BadRequestException("Vai trò không được để trống");
        }
        if (e.getPhone() == null || e.getPhone().isBlank()) {
            throw new BadRequestException("Số điện thoại không được để trống");
        }
        if (e.getSalary() == null || e.getSalary().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Lương không hợp lệ");
        }
    }
}
