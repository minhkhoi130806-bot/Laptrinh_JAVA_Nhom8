package com.restaurant.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * =========================================================================
 * LỚP TIỆN ÍCH JWT (JSON Web Token)
 * Người phụ trách: Ngô Minh Khôi (Tuần 3 - Bảo mật hệ thống)
 * 
 * Mục đích:
 * 1. Tạo chuỗi mã token khi người dùng đăng nhập thành công.
 * 2. Đọc thông tin (tên đăng nhập, quyền hạn) từ token mà client gửi lên.
 * 3. Kiểm tra token có hợp lệ không (chữ ký chuẩn, chưa hết hạn).
 * =========================================================================
 */
@Component
public class JwtUtil {

    // Chuỗi bí mật dùng để ký và xác thực mã token.
    // Chuỗi này phải dài ít nhất 32 ký tự (256-bit) để đảm bảo thuật toán HMAC-SHA256 an toàn.
    @Value("${app.security.jwt-secret:}")
    private String secretString;

    @Value("${app.security.jwt-expiration-ms:900000}")
    private long expirationTime;

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    // Tiền tố của khóa dev mặc định trong application.properties.
    private static final String DEV_SECRET_PREFIX = "han-seoul-dev-only-";

    @Autowired
    private Environment environment;

    // Không cho ứng dụng khởi động với khóa JWT rỗng/ngắn, tránh dùng khóa yếu ngoài ý muốn.
    @PostConstruct
    void validateConfiguration() {
        if (secretString == null || secretString.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET bắt buộc phải có tối thiểu 32 byte. Hãy cấu hình biến môi trường JWT_SECRET.");
        }
        if (secretString.startsWith(DEV_SECRET_PREFIX)) {
            // Khóa dev là công khai trong mã nguồn: cho phép khi chạy local, cấm ở môi trường prod.
            if (environment != null && environment.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("Không được dùng khóa JWT dev ở profile prod. Hãy đặt biến môi trường JWT_SECRET.");
            }
            log.warn("Đang dùng JWT secret DEV mặc định. Chỉ phù hợp chạy local; khi deploy hãy đặt JWT_SECRET riêng.");
        }
        if (expirationTime < 60_000L) {
            throw new IllegalStateException("JWT_EXPIRATION_MS phải tối thiểu 60000 (60 giây).");
        }
    }

    /**
     * Lấy khóa bí mật (SecretKey) từ chuỗi SECRET_STRING
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretString.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * BƯỚC 1: Tạo token JWT cho nhân viên sau khi đăng nhập đúng mật khẩu.
     * 
     * @param username Tên đăng nhập của nhân viên (ví dụ: "admin", "thu_ngan_1")
     * @param role     Quyền hạn của nhân viên (ví dụ: "MANAGER", "CASHIER", "WAITER", "CHEF")
     * @return Chuỗi token JWT định dạng: Header.Payload.Signature
     */
    public String generateToken(String username, String role) {
        // Thời điểm bắt đầu tạo token
        Date bayGio = new Date();
        // Thời điểm token hết hạn (sau 24h)
        Date hetHan = new Date(bayGio.getTime() + expirationTime);

        // Xây dựng token bằng thư viện JJWT
        return Jwts.builder()
                .subject(username)                 // Lưu username vào phần Subject của token
                .claim("role", role)               // Lưu thêm quyền (role) vào Payload
                .issuedAt(bayGio)                  // Thời gian cấp phát
                .expiration(hetHan)                // Thời gian hết hạn
                .signWith(getSigningKey())         // Ký điện tử bằng khóa bí mật
                .compact();                        // Nén lại thành chuỗi chuỗi hoàn chỉnh
    }

    /**
     * BƯỚC 2: Giải mã chuỗi token để lấy toàn bộ dữ liệu (Claims) bên trong.
     * 
     * @param token Chuỗi JWT do client gửi lên trong Header
     * @return Đối tượng Claims chứa thông tin, hoặc ném ra ngoại lệ nếu token sai/hết hạn
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())       // Dùng đúng khóa bí mật lúc tạo để kiểm tra chữ ký
                .build()
                .parseSignedClaims(token)          // Giải mã token
                .getPayload();                     // Lấy phần dữ liệu (payload)
    }

    /**
     * Lấy tên đăng nhập (username) từ token
     */
    public String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    /**
     * Lấy quyền hạn (role) từ token
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * BƯỚC 3: Kiểm tra token có hợp lệ không.
     * Token hợp lệ khi:
     * - Chữ ký khớp với khóa bí mật của server.
     * - Chưa quá thời gian hết hạn (expiration).
     * 
     * @param token Chuỗi JWT cần kiểm tra
     * @return true nếu hợp lệ, false nếu giả mạo hoặc đã hết hạn
     */
    public boolean validateToken(String token) {
        try {
            // Thử giải mã token, nếu chữ ký sai hoặc hết hạn thư viện sẽ tự ném ngoại lệ
            Claims claims = extractAllClaims(token);
            // Kiểm tra ngày hết hạn xem có nằm sau thời điểm hiện tại không
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            // Token không hợp lệ, bị sửa đổi trái phép hoặc đã quá hạn
            return false;
        }
    }
}
