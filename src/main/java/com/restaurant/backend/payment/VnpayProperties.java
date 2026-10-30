package com.restaurant.backend.payment;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Cấu hình cổng VNPay, đọc từ application.properties / biến môi trường (xem .env.example). */
@Component
public class VnpayProperties {
    @Value("${vnpay.tmn-code:}") private String tmnCode;
    @Value("${vnpay.hash-secret:}") private String hashSecret;
    @Value("${vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}") private String payUrl;
    @Value("${vnpay.api-url:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}") private String apiUrl;
    @Value("${vnpay.return-url:}") private String returnUrl;
    @Value("${vnpay.version:2.1.0}") private String version;
    @Value("${vnpay.command:pay}") private String command;
    @Value("${vnpay.order-type:other}") private String orderType;
    @Value("${vnpay.locale:vn}") private String locale;
    @Value("${vnpay.currency-code:VND}") private String currencyCode;

    /** Chỉ bật VNPay khi đủ khóa; thiếu khóa thì ẩn phương thức này ở giao diện. */
    public boolean isConfigured() {
        return notBlank(tmnCode) && notBlank(hashSecret) && notBlank(returnUrl);
    }

    /** Tên các cấu hình còn thiếu (không lộ giá trị). */
    public List<String> missing() {
        List<String> m = new ArrayList<>();
        if (!notBlank(tmnCode)) m.add("VNPAY_TMN_CODE");
        if (!notBlank(hashSecret)) m.add("VNPAY_HASH_SECRET");
        if (!notBlank(returnUrl)) m.add("VNPAY_RETURN_URL");
        return m;
    }

    @PostConstruct
    void logState() {
        Logger log = LoggerFactory.getLogger(VnpayProperties.class);
        if (isConfigured()) log.info("VNPay: ĐÃ cấu hình (tmnCode={}, return={})", tmnCode, returnUrl);
        else log.warn("VNPay: CHƯA cấu hình, thiếu {}", missing());
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }

    public String getTmnCode() { return tmnCode; }
    public String getHashSecret() { return hashSecret; }
    public String getPayUrl() { return payUrl; }
    public String getApiUrl() { return apiUrl; }
    public String getReturnUrl() { return returnUrl; }
    public String getVersion() { return version; }
    public String getCommand() { return command; }
    public String getOrderType() { return orderType; }
    public String getLocale() { return locale; }
    public String getCurrencyCode() { return currencyCode; }
}
