package com.restaurant.backend.payment;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Cấu hình cổng MoMo, đọc từ application.properties / biến môi trường (xem .env.example). */
@Component
public class MomoProperties {
    @Value("${momo.partner-code:}") private String partnerCode;
    @Value("${momo.access-key:}") private String accessKey;
    @Value("${momo.secret-key:}") private String secretKey;
    @Value("${momo.endpoint:}") private String endpoint;
    @Value("${momo.query-endpoint:}") private String queryEndpoint;
    @Value("${momo.return-url:}") private String returnUrl;
    @Value("${momo.ipn-url:}") private String ipnUrl;
    @Value("${momo.partner-name:Han Seoul}") private String partnerName;
    @Value("${momo.store-id:HanSeoulStore}") private String storeId;
    @Value("${momo.request-type:captureWallet}") private String requestType;

    /** Chỉ bật MoMo khi đủ khóa; thiếu khóa thì ẩn phương thức này ở giao diện. */
    public boolean isConfigured() {
        return notBlank(partnerCode) && notBlank(accessKey) && notBlank(secretKey)
                && notBlank(endpoint) && notBlank(returnUrl) && notBlank(ipnUrl);
    }

    /** Tên các cấu hình còn thiếu (không lộ giá trị). */
    public List<String> missing() {
        List<String> m = new ArrayList<>();
        if (!notBlank(partnerCode)) m.add("MOMO_PARTNER_CODE");
        if (!notBlank(accessKey)) m.add("MOMO_ACCESS_KEY");
        if (!notBlank(secretKey)) m.add("MOMO_SECRET_KEY");
        if (!notBlank(endpoint)) m.add("MOMO_ENDPOINT");
        if (!notBlank(returnUrl)) m.add("MOMO_RETURN_URL");
        if (!notBlank(ipnUrl)) m.add("MOMO_IPN_URL");
        return m;
    }

    @PostConstruct
    void logState() {
        Logger log = LoggerFactory.getLogger(MomoProperties.class);
        if (isConfigured()) log.info("MoMo: ĐÃ cấu hình (endpoint={}, ipn={}, return={})", endpoint, ipnUrl, returnUrl);
        else log.warn("MoMo: CHƯA cấu hình, thiếu {}", missing());
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }

    public String getPartnerCode() { return partnerCode; }
    public String getAccessKey() { return accessKey; }
    public String getSecretKey() { return secretKey; }
    public String getEndpoint() { return endpoint; }
    /** Mặc định suy ra từ endpoint tạo giao dịch: .../create -> .../query */
    public String getQueryEndpoint() {
        if (notBlank(queryEndpoint)) return queryEndpoint;
        return endpoint == null ? "" : endpoint.replaceAll("/create/?$", "/query");
    }
    public String getReturnUrl() { return returnUrl; }
    public String getIpnUrl() { return ipnUrl; }
    public String getPartnerName() { return partnerName; }
    public String getStoreId() { return storeId; }
    public String getRequestType() { return requestType; }
}
