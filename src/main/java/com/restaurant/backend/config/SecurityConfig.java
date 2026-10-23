package com.restaurant.backend.config;

import com.restaurant.backend.security.JwtAccessDeniedHandler;
import com.restaurant.backend.security.JwtAuthenticationEntryPoint;
import com.restaurant.backend.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * =========================================================================
 * CẤU HÌNH BẢO MẬT HỆ THỐNG (Spring Security + JWT)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 *
 * Mục đích:
 * 1. Cung cấp Bean mã hóa mật khẩu BCrypt (mã hóa mật khẩu 1 chiều an toàn).
 * 2. Cấu hình phân quyền truy cập cho từng Role:
 *    - MANAGER : Quản lý (toàn quyền)
 *    - CASHIER : Thu ngân (tạo đơn, gọi món, thanh toán hóa đơn)
 *    - WAITER  : Phục vụ (xem bàn, đặt bàn, tạo đơn, gọi món)
 *    - CHEF    : Đầu bếp (xem đơn hàng, món ăn cần làm, quản lý bếp)
 * 3. Đăng ký bộ lọc JwtFilter để chặn và kiểm tra token trước khi vào Controller.
 * 4. Xử lý trả lỗi 401 và 403 đúng chuẩn JSON của hệ thống (ApiError).
 * =========================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;

    // Tiêm các thành phần bảo mật qua Constructor
    public SecurityConfig(
            JwtFilter jwtFilter,
            JwtAuthenticationEntryPoint authenticationEntryPoint,
            JwtAccessDeniedHandler accessDeniedHandler) {
        this.jwtFilter = jwtFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /**
     * BEAN MÃ HÓA MẬT KHẨU BCRYPT
     * Thuật toán BCrypt tự động thêm salt (chuỗi ngẫu nhiên) khi băm mật khẩu,
     * giúp bảo mật tuyệt đối, chống tấn công từ điển (Rainbow table).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CẤU HÌNH BỘ LỌC BẢO MẬT HTTP (SecurityFilterChain)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tắt CSRF (Cross-Site Request Forgery) vì ứng dụng RESTful API dùng JWT,
            // không dùng cookie session nên không bị ảnh hưởng bởi CSRF.
            .csrf(csrf -> csrf.disable())

            // 2. Cấu hình Session là STATELESS (Không lưu phiên làm việc trên server).
            // Mỗi request đều độc lập và được xác thực qua Token JWT mang theo.
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 3. Xử lý trả về lỗi 401 và 403 đúng chuẩn JSON (ApiError)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint) // Trả 401 khi chưa đăng nhập / token lỗi
                .accessDeniedHandler(accessDeniedHandler)           // Trả 403 khi không đủ quyền hạn (role)
            )

            // 4. Cấu hình phân quyền cho các đường dẫn API
            .authorizeHttpRequests(auth -> auth
                // Cho phép trình duyệt gửi request OPTIONS (CORS Preflight) thoải mái
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Chỉ quản lý được cấp mới hoặc đổi quyền tài khoản nhân viên.
                .requestMatchers("/api/auth/employee-account").hasRole("MANAGER")
                // API đăng nhập nhân viên/quản lý được công khai.
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/uploads/**").permitAll()

                // Cho phép mọi người xem danh sách món ăn và danh mục thực đơn (GET)
                .requestMatchers(HttpMethod.GET, "/api/menu-items/**", "/api/categories/**").permitAll()

                // API Quản lý nhân viên: Chỉ có Quản lý (MANAGER) mới được thực hiện
                .requestMatchers("/api/employees/**").hasRole("MANAGER")

                // API Thanh toán & Hóa đơn: Quản lý (MANAGER) và Thu ngân (CASHIER) được thực hiện
                .requestMatchers("/api/invoices/**").hasAnyRole("MANAGER", "CASHIER")

                // API Đơn hàng & Gọi món: Cả Quản lý, Thu ngân, Bồi bàn, Đầu bếp đều cần dùng
                .requestMatchers("/api/orders/**", "/api/order-items/**").hasAnyRole("MANAGER", "CASHIER", "WAITER", "CHEF")

                // API quản lý chỉ dành cho tài khoản nhân viên/quản trị.
                // Tài khoản khách hàng chỉ xem được thực đơn công khai ở trên.
                .anyRequest().hasAnyRole("MANAGER", "CASHIER", "WAITER", "CHEF")
            )

            // 5. Thêm bộ lọc JwtFilter vào trước bộ lọc UsernamePasswordAuthenticationFilter mặc định
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
