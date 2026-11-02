package com.restaurant.backend.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.backend.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/** Gọi API ZaloPay v2 (tạo đơn hàng, truy vấn trạng thái). Có timeout rõ ràng, không log khóa bí mật. */
@Component
public class ZalopayClient {
    private static final Logger log = LoggerFactory.getLogger(ZalopayClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() { };
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ZalopayProperties props;
    private final RestClient http;

    public ZalopayClient(ZalopayProperties props) {
        this.props = props;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    /** Tạo đơn hàng ZaloPay; trả về orderUrl để chuyển hướng khách sang cổng thanh toán. */
    public ZalopayGatewayResult create(String appTransId, long amount, String description, String appUser) {
        if (!props.isConfigured()) {
            throw new PaymentGatewayException("ZaloPay chưa được cấu hình đầy đủ");
        }

        int appId = props.getAppId();
        long appTime = System.currentTimeMillis();
        String user = (appUser == null || appUser.isBlank()) ? "HanSeoulCustomer" : appUser;
        String item = "[]";

        // embed_data chứa redirecturl để ZaloPay chuyển hướng về sau thanh toán
        Map<String, String> embedMap = Collections.singletonMap("redirecturl", props.getReturnUrl());
        String embedData;
        try {
            embedData = JSON.writeValueAsString(embedMap);
        } catch (Exception e) {
            embedData = "{\"redirecturl\":\"" + props.getReturnUrl() + "\"}";
        }

        String mac = ZalopaySignature.forCreate(props.getKey1(), appId, appTransId, user, amount, appTime, embedData, item);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("app_id", String.valueOf(appId));
        formData.add("app_user", user);
        formData.add("app_trans_id", appTransId);
        formData.add("app_time", String.valueOf(appTime));
        formData.add("amount", String.valueOf(amount));
        formData.add("item", item);
        formData.add("description", description);
        formData.add("embed_data", embedData);
        formData.add("bank_code", "");
        if (props.getCallbackUrl() != null && !props.getCallbackUrl().isBlank()) {
            formData.add("callback_url", props.getCallbackUrl());
        }
        formData.add("mac", mac);

        Map<String, Object> res = post(props.getEndpoint(), formData);
        return toResult(res);
    }

    /** Truy vấn trạng thái đơn hàng trực tiếp từ ZaloPay theo app_trans_id. */
    public ZalopayGatewayResult query(String appTransId) {
        if (!props.isConfigured()) {
            throw new PaymentGatewayException("ZaloPay chưa được cấu hình đầy đủ");
        }

        int appId = props.getAppId();
        String mac = ZalopaySignature.forQuery(props.getKey1(), appId, appTransId);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("app_id", String.valueOf(appId));
        formData.add("app_trans_id", appTransId);
        formData.add("mac", mac);

        Map<String, Object> res = post(props.getQueryEndpoint(), formData);
        return toResult(res);
    }

    private Map<String, Object> post(String url, MultiValueMap<String, String> formData) {
        try {
            Map<String, Object> res = http.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(formData)
                    .retrieve()
                    .body(MAP_TYPE);
            if (res == null) throw new PaymentGatewayException("ZaloPay trả về phản hồi rỗng");
            return res;
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String resp = e.getResponseBodyAsString();
            try {
                Map<String, Object> errorBody = JSON.readValue(resp, new TypeReference<Map<String, Object>>() { });
                if (errorBody.get("return_code") instanceof Number) {
                    log.warn("ZaloPay trả HTTP {} return_code={} return_message={}",
                            e.getStatusCode().value(), errorBody.get("return_code"), errorBody.get("return_message"));
                    return errorBody;
                }
            } catch (Exception ignored) { }
            log.error("Lỗi HTTP khi gọi ZaloPay ({}): {}", e.getStatusCode(), resp);
            throw new PaymentGatewayException("Lỗi kết nối tới ZaloPay: HTTP " + e.getStatusCode().value());
        } catch (Exception e) {
            log.error("Lỗi mạng khi kết nối tới ZaloPay {}: {}", url, e.getMessage());
            throw new PaymentGatewayException("Không thể kết nối đến cổng ZaloPay");
        }
    }

    private ZalopayGatewayResult toResult(Map<String, Object> res) {
        int returnCode = res.get("return_code") instanceof Number n ? n.intValue() : -1;
        String returnMessage = res.get("return_message") == null ? null : String.valueOf(res.get("return_message"));
        int subReturnCode = res.get("sub_return_code") instanceof Number n ? n.intValue() : returnCode;
        String subReturnMessage = res.get("sub_return_message") == null ? null : String.valueOf(res.get("sub_return_message"));
        String orderUrl = res.get("order_url") == null ? null : String.valueOf(res.get("order_url"));
        Long zpTransId = res.get("zp_trans_id") instanceof Number n ? n.longValue() : null;
        Long amount = res.get("amount") instanceof Number n ? n.longValue() : null;
        Boolean isProcessing = res.get("is_processing") instanceof Boolean b ? b : null;

        return new ZalopayGatewayResult(returnCode, returnMessage, subReturnCode, subReturnMessage,
                orderUrl, zpTransId, amount, isProcessing);
    }
}
