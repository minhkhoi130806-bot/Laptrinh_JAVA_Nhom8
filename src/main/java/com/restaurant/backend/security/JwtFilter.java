package com.restaurant.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * =========================================================================
 * BỘ LỌC BẢO MẬT JWT (OncePerRequestFilter)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 * 
 * Mục đích:
 * Bộ lọc này giống như "Bảo vệ gác cổng". Mỗi khi có bất kỳ request HTTP nào
 * gửi từ client (React, Postman...) lên server, bộ lọc này sẽ chạy trước tiên:
 * 1. Kiểm tra xem request có mang theo "Thẻ thông hành" (Token JWT) trong Header không.
 * 2. Header chuẩn có dạng: "Authorization: Bearer <chuỗi_token>"
 * 3. Nếu token hợp lệ: Lấy thông tin (username, role) và nạp vào SecurityContext của Spring.
 * 4. Cho phép request tiếp tục đi vào Controller để xử lý.
 * =========================================================================
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    // Tiêm (Inject) lớp tiện ích JwtUtil vào bộ lọc
    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Bước 1: Lấy giá trị của Header mang tên "Authorization" từ request gửi lên
        String authHeader = request.getHeader("Authorization");

        String token = null;
        String username = null;

        // Bước 2: Kiểm tra Header có tồn tại và có bắt đầu bằng tiền tố "Bearer " hay không
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // Cắt bỏ 7 ký tự đầu tiên ("Bearer ") để lấy ra chuỗi token thực sự
            token = authHeader.substring(7);

            // Kiểm tra token có hợp lệ không (chữ ký chuẩn và chưa hết hạn)
            if (jwtUtil.validateToken(token)) {
                // Lấy tên đăng nhập được lưu trong token
                username = jwtUtil.extractUsername(token);
            }
        }

        // Bước 3: Nếu token hợp lệ và hiện tại người dùng chưa được xác thực trong phiên này
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Lấy vai trò (role) từ token, ví dụ: "MANAGER", "CASHIER", "WAITER", "CHEF"
            String role = jwtUtil.extractRole(token);

            // Chuyển role thành quyền hạn trong Spring Security (theo quy ước có tiền tố "ROLE_")
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            // Tạo đối tượng xác thực (Authentication) của Spring Security
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);

            // Gắn thêm thông tin chi tiết của request (địa chỉ IP, session...)
            authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            // Lưu thông tin người dùng đã xác thực vào SecurityContextHolder
            // Từ đây, các Controller hoặc Service có thể biết được ai đang thực hiện hành động
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }

        // Bước 4: Chuyển request cho các bộ lọc tiếp theo và Controller xử lý
        filterChain.doFilter(request, response);
    }
}
