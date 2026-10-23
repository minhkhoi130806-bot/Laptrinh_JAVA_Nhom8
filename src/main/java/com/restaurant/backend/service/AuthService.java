package com.restaurant.backend.service;

import com.restaurant.backend.dto.LoginRequest;
import com.restaurant.backend.dto.LoginResponse;
import com.restaurant.backend.dto.EmployeeAccountRequest;
import com.restaurant.backend.dto.EmployeeAccountResponse;

/**
 * Interface định nghĩa các chức năng xác thực và đăng nhập tài khoản.
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 */
public interface AuthService {

    /**
     * Xử lý đăng nhập tài khoản nhân viên.
     * 
     * @param request Chứa username và password gửi lên từ form đăng nhập
     * @return LoginResponse chứa mã token JWT và thông tin nhân viên
     */
    LoginResponse login(LoginRequest request);

    EmployeeAccountResponse createEmployeeAccount(EmployeeAccountRequest request);
}
