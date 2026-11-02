package com.restaurant.backend.payment;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Cấu hình cổng ZaloPay, đọc từ application.properties / biến môi trường (xem .env.example). */
@Component
public class ZalopayProperties {
    @Value("${zalopay.app-id:}") private String appIdStr;
    @Value("${zalopay.key1:}") private String key1;
    @Value("${zalopay.key2:}") private String key2;
    @Value("${zalopay.endpoint:https://sb-openapi.zalopay.vn/v2/create}") private String endpoint;
    @Value("${zalopay.query-endpoint:https://sb-openapi.zalopay.vn/v2/query}") private String queryEndpoint;
    @Value("${zalopay.return-url:}") private String returnUrl;
    @Value("${zalopay.callback-url:}") private String callbackUrl;

    public Integer getAppId() {
        if (appIdStr == null || appIdStr.isBlank()) return null;
        try {
            return Integer.parseInt(appIdStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Chỉ bật ZaloPay khi đủ cấu hình: appId, key1, key2, returnUrl. */
    public boolean isConfigured() {
        return getAppId() != null && notBlank(key1) && notBlank(key2) && notBlank(returnUrl);
    }

    /** Tên các cấu hình còn thiếu (không lộ giá trị). */
    public List<String> missing() {
        List<String> m = new ArrayList<>();
        if (getAppId() == null) m.add("ZALOPAY_APP_ID");
        if (!notBlank(key1)) m.add("ZALOPAY_KEY1");
        if (!notBlank(key2)) m.add("ZALOPAY_KEY2");
        if (!notBlank(returnUrl)) m.add("ZALOPAY_RETURN_URL");
        return m;
    }

    @PostConstruct
    void logState() {
        Logger log = LoggerFactory.getLogger(ZalopayProperties.class);
        if (isConfigured()) log.info("ZaloPay: ĐÃ cấu hình (appId={}, return={})", appIdStr, returnUrl);
        else log.warn("ZaloPay: CHƯA cấu hình, thiếu {}", missing());
    }

    private static boolean notBlank(String value) { return value != null && !value.isBlank(); }

    public String getAppIdStr() { return appIdStr; }
    public String getKey1() { return key1; }
    public String getKey2() { return key2; }
    public String getEndpoint() { return endpoint; }
    public String getQueryEndpoint() { return queryEndpoint; }
    public String getReturnUrl() { return returnUrl; }
    public String getCallbackUrl() { return callbackUrl; }
}
