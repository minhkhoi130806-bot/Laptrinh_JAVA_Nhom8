package com.restaurant.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

/** Tạo và kiểm tra chữ ký HMAC-SHA256 theo đặc tả MoMo v2. Lớp thuần Java, không phụ thuộc Spring. */
public final class MomoSignature {
    // Thứ tự khóa bắt buộc (a-z) theo tài liệu MoMo.
    private static final String[] CREATE_KEYS = {"accessKey", "amount", "extraData", "ipnUrl", "orderId", "orderInfo",
            "partnerCode", "redirectUrl", "requestId", "requestType"};
    private static final String[] QUERY_KEYS = {"accessKey", "orderId", "partnerCode", "requestId"};
    private static final String[] IPN_KEYS = {"accessKey", "amount", "extraData", "message", "orderId", "orderInfo",
            "orderType", "partnerCode", "payType", "requestId", "responseTime", "resultCode", "transId"};

    private MomoSignature() { }

    public static String forCreate(String secretKey, Map<String, ?> values) { return sign(secretKey, raw(values, CREATE_KEYS)); }
    public static String forQuery(String secretKey, Map<String, ?> values) { return sign(secretKey, raw(values, QUERY_KEYS)); }
    public static String forIpn(String secretKey, Map<String, ?> values) { return sign(secretKey, raw(values, IPN_KEYS)); }

    /** "k1=v1&k2=v2..." theo đúng thứ tự khóa; giá trị thiếu coi là chuỗi rỗng. */
    static String raw(Map<String, ?> values, String[] keys) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) sb.append('&');
            Object v = values.get(keys[i]);
            sb.append(keys[i]).append('=').append(v == null ? "" : String.valueOf(v));
        }
        return sb.toString();
    }

    static String sign(String secretKey, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA256", e);
        }
    }

    /** So sánh thời gian hằng số để tránh timing attack. */
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
