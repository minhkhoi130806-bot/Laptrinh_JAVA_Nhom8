package com.restaurant.backend.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * Tạo và kiểm tra chữ ký HMAC-SHA512 theo chuẩn tài liệu chính thức của VNPay (v2.1.0).
 */
public final class VnpaySignature {

    private VnpaySignature() { }

    /**
     * Tạo chuỗi hash data từ các tham số (đã sắp xếp A-Z và URLEncode value theo US_ASCII).
     * Đúng chuẩn tài liệu VNPay Java Demo.
     */
    public static String hashAllFields(String hashSecret, Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty() && fieldName.startsWith("vnp_")) {
                if (sb.length() > 0) {
                    sb.append("&");
                }
                sb.append(fieldName)
                  .append("=")
                  .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }
        return hmacSHA512(hashSecret, sb.toString());
    }

    /**
     * Xác thực chữ ký khi VNPay gọi Return URL hoặc IPN.
     * Bỏ vnp_SecureHash và vnp_SecureHashType trước khi băm.
     */
    public static boolean verify(String hashSecret, Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) return false;

        Map<String, String> toVerify = new HashMap<>(params);
        toVerify.remove("vnp_SecureHash");
        toVerify.remove("vnp_SecureHashType");

        String computed = hashAllFields(hashSecret, toVerify);
        return MessageDigest.isEqual(
                computed.toLowerCase().getBytes(StandardCharsets.UTF_8),
                receivedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    public static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể tạo chữ ký HMAC-SHA512", e);
        }
    }
}
