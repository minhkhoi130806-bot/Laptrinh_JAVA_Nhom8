package com.restaurant.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Tạo và kiểm tra chữ ký HMAC-SHA256 theo chuẩn tài liệu ZaloPay v2. Lớp thuần Java, không phụ thuộc Spring. */
public final class ZalopaySignature {

    private ZalopaySignature() { }

    /**
     * Chữ ký khi tạo đơn hàng (Create Order):
     * data = app_id + "|" + app_trans_id + "|" + app_user + "|" + amount + "|" + app_time + "|" + embed_data + "|" + item
     * mac = HmacSHA256(key1, data)
     */
    public static String forCreate(String key1, int appId, String appTransId, String appUser, long amount, long appTime, String embedData, String item) {
        String data = appId + "|" + appTransId + "|" + appUser + "|" + amount + "|" + appTime + "|" + embedData + "|" + item;
        return sign(key1, data);
    }

    /**
     * Chữ ký khi truy vấn trạng thái đơn hàng (Query Order Status):
     * data = app_id + "|" + app_trans_id + "|" + key1
     * mac = HmacSHA256(key1, data)
     */
    public static String forQuery(String key1, int appId, String appTransId) {
        String data = appId + "|" + appTransId + "|" + key1;
        return sign(key1, data);
    }

    /**
     * Chữ ký xác thực Callback (IPN):
     * mac = HmacSHA256(key2, data)
     */
    public static String forCallback(String key2, String dataJson) {
        return sign(key2, dataJson);
    }

    /**
     * Xác thực Callback từ ZaloPay server gửi sang.
     */
    public static boolean verifyCallback(String key2, String dataJson, String requestMac) {
        if (key2 == null || dataJson == null || requestMac == null) return false;
        String calculated = forCallback(key2, dataJson);
        return matches(calculated, requestMac);
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
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA256 ZaloPay", e);
        }
    }

    /** So sánh thời gian hằng số để tránh timing attack. */
    public static boolean matches(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actual.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
    }
}
