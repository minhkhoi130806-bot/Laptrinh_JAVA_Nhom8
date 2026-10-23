package com.restaurant.backend.controller;

import com.restaurant.backend.dto.LoginRequest;
import com.restaurant.backend.dto.LoginResponse;
import com.restaurant.backend.dto.EmployeeAccountRequest;
import com.restaurant.backend.dto.EmployeeAccountResponse;
import com.restaurant.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * =========================================================================
 * CONTROLLER XÁC THỰC VÀ ĐĂNG NHẬP (/api/auth)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 * 
 * Mục đích:
 * Cung cấp API công khai cho nhân viên đăng nhập vào hệ thống:
 * - URL: POST /api/auth/login
 * - Body gửi lên: { "username": "admin", "password": "123" }
 * - Kết quả trả về: Token JWT và thông tin tài khoản
 * =========================================================================
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // Tiêm AuthService vào Controller qua Constructor
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * API Đăng nhập tài khoản
     * Endpoint: POST /api/auth/login
     * 
     * @param request Dữ liệu tên đăng nhập và mật khẩu
     * @return Đối tượng LoginResponse chứa token JWT nếu đăng nhập thành công
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Gọi tầng Service để kiểm tra mật khẩu và tạo Token
        LoginResponse response = authService.login(request);
        // Trả về mã HTTP 200 OK cùng dữ liệu đăng nhập
        return ResponseEntity.ok(response);
    }

    @PostMapping("/employee-account")
    public ResponseEntity<EmployeeAccountResponse> createEmployeeAccount(
            @Valid @RequestBody EmployeeAccountRequest request) {
        EmployeeAccountResponse response = authService.createEmployeeAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
