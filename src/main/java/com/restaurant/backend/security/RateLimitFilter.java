package com.restaurant.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Giới hạn tốc độ cơ bản theo IP cho endpoint công khai; triển khai nhiều instance nên dùng Redis/Bucket4j. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final long WINDOW_MS = 60_000L;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();
    // Khởi tạo trực tiếp để filter không phụ thuộc vào ObjectMapper bean của Spring.
    // Điều này cũng tương thích với cấu hình Spring Boot 4 hiện tại của dự án.
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.startsWith("/api/auth/") || path.equals("/api/reservations/public")
                || path.startsWith("/api/contact") || path.startsWith("/api/event-inquiries")
                || path.equals("/api/payments/momo/create") || path.equals("/api/payments/momo/status"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        String path = request.getRequestURI();
        int limit = path.startsWith("/api/auth/") ? 10 : 20;
        long now = System.currentTimeMillis();
        String key = ip + "|" + path;
        Counter counter = counters.compute(key, (ignored, current) ->
                current == null || now - current.windowStart >= WINDOW_MS ? new Counter(now) : current);
        if (counter.count.incrementAndGet() > limit) {
            long retrySeconds = Math.max(1, (counter.windowStart + WINDOW_MS - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", Long.toString(retrySeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", Instant.now().toString(), "status", 429, "error", "Too Many Requests",
                    "message", "Bạn thao tác quá nhanh. Vui lòng thử lại sau.", "path", path));
            return;
        }
        // Dọn bộ đếm cũ định kỳ theo cơ hội, tránh map tăng vô hạn trong môi trường dev.
        if (counters.size() > 10_000) counters.entrySet().removeIf(entry -> now - entry.getValue().windowStart > WINDOW_MS * 2);
        chain.doFilter(request, response);
    }

    private static final class Counter {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger();
        private Counter(long windowStart) { this.windowStart = windowStart; }
    }
}
