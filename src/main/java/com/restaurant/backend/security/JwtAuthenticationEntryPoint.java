package com.restaurant.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * =========================================================================
 * XỬ LÝ LỖI 401 UNAUTHORIZED (Chưa đăng nhập hoặc Token không hợp lệ)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 * 
 * Mục đích:
 * Khi người dùng gọi vào một API yêu cầu đăng nhập nhưng:
 * - Không gửi kèm token trong Header Authorization.
 * - Gửi token sai, token hết hạn, hoặc bị sửa đổi.
 * 
 * Hệ thống sẽ chặn lại và trả về chuỗi JSON lỗi chuẩn theo đúng định dạng ApiError:
 * {
 *   "timestamp": "2026-10-23T...",
 *   "status": 401,
 *   "error": "Unauthorized",
 *   "message": "Bạn cần đăng nhập bằng mã token hợp lệ để thực hiện thao tác này",
 *   "path": "/api/orders"
 * }
 * =========================================================================
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // Bước 1: Thiết lập mã phản hồi HTTP là 401 (Chưa đăng nhập)
        response.setStatus(HttpStatus.UNAUTHORIZED.value());

        // Thiết lập kiểu dữ liệu trả về là JSON và hỗ trợ tiếng Việt UTF-8
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Bước 2: Tạo chuỗi JSON lỗi chuẩn ApiError một cách đơn giản, dễ hiểu
        String thoiGian = LocalDateTime.now().toString();
        int maLoi = HttpStatus.UNAUTHORIZED.value();
        String tenLoi = HttpStatus.UNAUTHORIZED.getReasonPhrase();
        String thongBao = "Bạn cần đăng nhập bằng mã token hợp lệ để thực hiện thao tác này";
        String duongDan = request.getRequestURI();

        String jsonLoi = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                thoiGian, maLoi, tenLoi, thongBao, duongDan
        );

        // Bước 3: Ghi dữ liệu JSON ra cho phía client
        response.getWriter().write(jsonLoi);
    }
}
