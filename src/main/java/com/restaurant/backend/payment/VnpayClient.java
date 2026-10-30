package com.restaurant.backend.payment;

import com.restaurant.backend.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tạo URL thanh toán VNPay theo chuẩn tài liệu chính thức của VNPay Java Demo.
 */
@Component
public class VnpayClient {
    private static final Logger log = LoggerFactory.getLogger(VnpayClient.class);
    private static final DateTimeFormatter VN_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties props;

    public VnpayClient(VnpayProperties props) {
        this.props = props;
    }

    /**
     * Tạo URL thanh toán VNPay.
     * @param vnpTxnRef mã giao dịch duy nhất
     * @param amount số tiền VND
     * @param orderInfo mô tả đơn hàng
     * @param ipAddress IP của khách hàng
     * @return URL để redirect khách sang cổng VNPay
     */
    public String createPayUrl(String vnpTxnRef, long amount, String orderInfo, String ipAddress) {
        if (!props.isConfigured()) {
            throw new PaymentGatewayException("VNPay chưa được cấu hình trên máy chủ");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", props.getVersion());
        vnp_Params.put("vnp_Command", props.getCommand());
        vnp_Params.put("vnp_TmnCode", props.getTmnCode());
        // Số tiền theo chuẩn VNPay nhân 100
        vnp_Params.put("vnp_Amount", String.valueOf(amount * 100));
        vnp_Params.put("vnp_CurrCode", props.getCurrencyCode());
        vnp_Params.put("vnp_TxnRef", vnpTxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", props.getOrderType());
        vnp_Params.put("vnp_Locale", props.getLocale());
        vnp_Params.put("vnp_ReturnUrl", props.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", (ipAddress == null || ipAddress.isBlank()) ? "127.0.0.1" : ipAddress);
        vnp_Params.put("vnp_CreateDate", now.format(VN_DATE));
        vnp_Params.put("vnp_ExpireDate", now.plusMinutes(15).format(VN_DATE));

        // Sắp xếp các trường theo thứ tự A-Z
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                // Build hash data
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build query
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VnpaySignature.hmacSHA512(props.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        String paymentUrl = props.getPayUrl() + "?" + queryUrl;
        log.info("VNPay URL created for txnRef={}", vnpTxnRef);
        return paymentUrl;
    }
}
