package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.ListScope;
import com.restaurant.backend.entity.AccountType;
import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.entity.EmployeeRole;
import com.restaurant.backend.entity.UserAccount;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.exception.NotFoundException;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.repository.UserAccountRepository;
import com.restaurant.backend.service.EmployeeService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
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
        if (userAccountRepository.existsByUsername(employee.getUsername())) {
            throw new BadRequestException("Username đã được sử dụng");
        }
        employee.setId(null);
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        Employee savedEmployee = employeeRepository.save(employee);
        saveLoginAccount(savedEmployee, savedEmployee.getPassword());
        return savedEmployee;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> getAll(ListScope scope) {
        if (scope == ListScope.ALL) {
            return employeeRepository.findAll();
        }
        return employeeRepository.findByDeleted(scope == ListScope.TRASH);
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
        UserAccount accountByUsername = userAccountRepository.findByUsername(data.getUsername()).orElse(null);
        if (accountByUsername != null
                && (accountByUsername.getEmployee() == null
                || !accountByUsername.getEmployee().getId().equals(id))) {
            throw new BadRequestException("Username đã được sử dụng");
        }
        existing.setUsername(data.getUsername());
        existing.setFullName(data.getFullName());
        existing.setRole(data.getRole());
        existing.setPhone(data.getPhone());
        existing.setSalary(data.getSalary());
        // Chỉ đổi mật khẩu khi request có gửi password mới
        if (data.getPassword() != null && !data.getPassword().isBlank()) {
            existing.setPassword(passwordEncoder.encode(data.getPassword()));
        }
        Employee savedEmployee = employeeRepository.save(existing);
        saveLoginAccount(savedEmployee, savedEmployee.getPassword());
        return savedEmployee;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Employee existing = getById(id);
        existing.setDeleted(true);
        existing.setDeletedAt(LocalDateTime.now());
        employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public Employee restore(Long id) {
        Employee existing = getById(id);
        existing.setDeleted(false);
        existing.setDeletedAt(null);
        return employeeRepository.save(existing);
    }

    @Override
    @Transactional
    public void purge(Long id) {
        Employee existing = getById(id);
        if (!Boolean.TRUE.equals(existing.getDeleted())) {
            throw new BadRequestException("Chỉ xóa vĩnh viễn được nhân viên đang trong thùng rác");
        }
        userAccountRepository.findByEmployeeId(id).ifPresent(userAccountRepository::delete);
        employeeRepository.delete(existing);
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

    private void saveLoginAccount(Employee employee, String encodedPassword) {
        if (encodedPassword == null || encodedPassword.isBlank()) {
            throw new BadRequestException("Mật khẩu nhân viên không được để trống");
        }
        if (!encodedPassword.startsWith("$2")) {
            encodedPassword = passwordEncoder.encode(encodedPassword);
            employee.setPassword(encodedPassword);
            employeeRepository.save(employee);
        }

        UserAccount account = userAccountRepository.findByEmployeeId(employee.getId()).orElse(null);
        if (account == null) {
            account = new UserAccount();
            account.setEmployee(employee);
        }

        account.setUsername(employee.getUsername());
        account.setPassword(encodedPassword);
        account.setEmployeeRole(employee.getRole());
        account.setAccountType(employee.getRole() == EmployeeRole.MANAGER
                ? AccountType.ADMIN
                : AccountType.EMPLOYEE);
        userAccountRepository.save(account);
    }
}
