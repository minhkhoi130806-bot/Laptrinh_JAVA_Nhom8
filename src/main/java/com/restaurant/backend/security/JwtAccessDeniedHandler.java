package com.restaurant.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * =========================================================================
 * XỬ LÝ LỖI 403 FORBIDDEN (Không đủ quyền hạn)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 * 
 * Mục đích:
 * Khi người dùng đã đăng nhập (có token hợp lệ) nhưng tài khoản không có quyền
 * (role) để thực hiện chức năng được yêu cầu.
 * Ví dụ: Nhân viên bồi bàn (WAITER) gọi API của Quản lý (MANAGER).
 * 
 * Hệ thống sẽ trả về chuỗi JSON lỗi chuẩn theo đúng định dạng ApiError:
 * {
 *   "timestamp": "2026-10-23T...",
 *   "status": 403,
 *   "error": "Forbidden",
 *   "message": "Bạn không có quyền thực hiện chức năng này",
 *   "path": "/api/employees"
 * }
 * =========================================================================
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        // Bước 1: Thiết lập mã phản hồi HTTP là 403 (Bị cấm / Không đủ quyền)
        response.setStatus(HttpStatus.FORBIDDEN.value());

        // Thiết lập kiểu dữ liệu trả về là JSON và hỗ trợ tiếng Việt UTF-8
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Bước 2: Tạo chuỗi JSON lỗi chuẩn ApiError một cách đơn giản, dễ hiểu
        String thoiGian = LocalDateTime.now().toString();
        int maLoi = HttpStatus.FORBIDDEN.value();
        String tenLoi = HttpStatus.FORBIDDEN.getReasonPhrase();
        String thongBao = "Bạn không có quyền (role) để thực hiện chức năng này";
        String duongDan = request.getRequestURI();

        String jsonLoi = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                thoiGian, maLoi, tenLoi, thongBao, duongDan
        );

        // Bước 3: Ghi dữ liệu JSON ra cho phía client
        response.getWriter().write(jsonLoi);
    }
}
