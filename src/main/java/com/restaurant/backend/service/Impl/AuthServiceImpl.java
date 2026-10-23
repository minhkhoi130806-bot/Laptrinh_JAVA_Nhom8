package com.restaurant.backend.service.Impl;

import com.restaurant.backend.dto.LoginRequest;
import com.restaurant.backend.dto.LoginResponse;
import com.restaurant.backend.dto.EmployeeAccountRequest;
import com.restaurant.backend.dto.EmployeeAccountResponse;
import com.restaurant.backend.entity.AccountType;
import com.restaurant.backend.entity.Employee;
import com.restaurant.backend.entity.EmployeeRole;
import com.restaurant.backend.entity.UserAccount;
import com.restaurant.backend.exception.BadRequestException;
import com.restaurant.backend.repository.EmployeeRepository;
import com.restaurant.backend.repository.UserAccountRepository;
import com.restaurant.backend.security.JwtUtil;
import com.restaurant.backend.service.AuthService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UserAccountRepository userAccountRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil) {
        this.userAccountRepository = userAccountRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String username = request.username().trim();
        UserAccount account = userAccountRepository.findByUsername(username).orElse(null);

        if (account != null) {
            return loginAccount(account, request.password());
        }

        // Import old employee accounts the first time they successfully sign in.
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> invalidCredentials());
        if (Boolean.TRUE.equals(employee.getDeleted()) || !passwordMatches(request.password(), employee.getPassword())) {
            throw invalidCredentials();
        }

        account = new UserAccount();
        account.setUsername(employee.getUsername());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setEmployee(employee);
        account.setEmployeeRole(employee.getRole());
        account.setAccountType(employee.getRole() == EmployeeRole.MANAGER
                ? AccountType.ADMIN
                : AccountType.EMPLOYEE);
        userAccountRepository.save(account);

        // Keep the legacy employee password in sync as a BCrypt hash.
        employee.setPassword(account.getPassword());
        employeeRepository.save(employee);

        return employeeResponse(account);
    }

    @Override
    @Transactional
    public EmployeeAccountResponse createEmployeeAccount(EmployeeAccountRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new BadRequestException("Không tìm thấy nhân viên"));
        if (Boolean.TRUE.equals(employee.getDeleted())) {
            throw new BadRequestException("Không thể cấp tài khoản cho nhân viên đã nghỉ hoặc bị vô hiệu hóa");
        }

        String username = request.username().trim();
        if (employeeRepository.existsByUsernameAndIdNot(username, employee.getId())) {
            throw new BadRequestException("Tên đăng nhập đã được nhân viên khác sử dụng");
        }

        UserAccount account = userAccountRepository.findByEmployeeId(employee.getId()).orElse(null);
        UserAccount usernameOwner = userAccountRepository.findByUsername(username).orElse(null);
        if (usernameOwner != null && (account == null || !usernameOwner.getId().equals(account.getId()))) {
            throw new BadRequestException("Tên đăng nhập đã được sử dụng");
        }

        employee.setUsername(username);
        employee.setPassword(passwordEncoder.encode(request.password()));
        employee.setRole(request.role());
        employeeRepository.save(employee);

        if (account == null) {
            account = new UserAccount();
            account.setEmployee(employee);
        }
        account.setUsername(username);
        account.setPassword(employee.getPassword());
        account.setEmployeeRole(request.role());
        account.setAccountType(request.role() == EmployeeRole.MANAGER
                ? AccountType.ADMIN
                : AccountType.EMPLOYEE);
        userAccountRepository.save(account);

        return new EmployeeAccountResponse(
                employee.getId(),
                account.getUsername(),
                employee.getFullName(),
                request.role().name(),
                account.getAccountType().name()
        );
    }

    private LoginResponse loginAccount(UserAccount account, String password) {
        if (!passwordEncoder.matches(password, account.getPassword())) {
            throw invalidCredentials();
        }

        if (account.getAccountType() == AccountType.CUSTOMER) {
            throw new BadRequestException("Cổng đăng nhập này chỉ dành cho nhân viên và quản lý");
        }

        return employeeResponse(account);
    }

    private LoginResponse employeeResponse(UserAccount account) {
        Employee employee = account.getEmployee();
        if (employee == null || Boolean.TRUE.equals(employee.getDeleted())) {
            throw new BadRequestException("Tài khoản nhân viên đã bị vô hiệu hóa");
        }

        EmployeeRole currentRole = employee.getRole();
        if (currentRole == null) {
            throw new BadRequestException("Tài khoản nhân viên chưa được phân quyền");
        }
        AccountType currentAccountType = currentRole == EmployeeRole.MANAGER
                ? AccountType.ADMIN
                : AccountType.EMPLOYEE;
        if (account.getEmployeeRole() != currentRole || account.getAccountType() != currentAccountType) {
            account.setEmployeeRole(currentRole);
            account.setAccountType(currentAccountType);
            userAccountRepository.save(account);
        }

        String role = currentRole.name();
        return new LoginResponse(
                jwtUtil.generateToken(account.getUsername(), role),
                employee.getId(),
                account.getUsername(),
                employee.getFullName(),
                role,
                currentAccountType.name()
        );
    }

    private boolean passwordMatches(String enteredPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        return storedPassword.startsWith("$2")
                ? passwordEncoder.matches(enteredPassword, storedPassword)
                : enteredPassword.equals(storedPassword);
    }

    private BadRequestException invalidCredentials() {
        return new BadRequestException("Tên đăng nhập hoặc mật khẩu không chính xác");
    }
}
