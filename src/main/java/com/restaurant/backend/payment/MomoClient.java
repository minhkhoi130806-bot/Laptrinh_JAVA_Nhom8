package com.restaurant.backend.payment;

import com.restaurant.backend.exception.PaymentGatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Gọi API MoMo (tạo giao dịch, truy vấn trạng thái). Có timeout rõ ràng, không log khóa bí mật. */
@Component
public class MomoClient {
    private static final Logger log = LoggerFactory.getLogger(MomoClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() { };
    private static final ObjectMapper JSON = new ObjectMapper();

    private final MomoProperties props;
    private final RestClient http;

    public MomoClient(MomoProperties props) {
        this.props = props;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    /** Tạo giao dịch; MoMo trả payUrl để chuyển hướng khách sang trang thanh toán. */
    public MomoGatewayResult create(String momoOrderId, long amount, String orderInfo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("partnerName", props.getPartnerName());
        body.put("storeId", props.getStoreId());
        body.put("requestId", momoOrderId);
        body.put("amount", amount);
        body.put("orderId", momoOrderId);
        body.put("orderInfo", orderInfo);
        body.put("redirectUrl", props.getReturnUrl());
        body.put("ipnUrl", props.getIpnUrl());
        body.put("lang", "vi");
        body.put("requestType", props.getRequestType());
        body.put("autoCapture", true);
        body.put("extraData", "");
        // accessKey chỉ tham gia ký, không gửi trong body.
        Map<String, Object> toSign = new LinkedHashMap<>(body);
        toSign.put("accessKey", props.getAccessKey());
        body.put("signature", MomoSignature.forCreate(props.getSecretKey(), toSign));

        Map<String, Object> res = post(props.getEndpoint(), body);
        return toResult(res);
    }

    /** Truy vấn trạng thái giao dịch trực tiếp từ MoMo (nguồn tin cậy hơn tham số trên URL redirect). */
    public MomoGatewayResult query(String momoOrderId) {
        String requestId = UUID.randomUUID().toString();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", props.getPartnerCode());
        body.put("requestId", requestId);
        body.put("orderId", momoOrderId);
        body.put("lang", "vi");
        Map<String, Object> toSign = new LinkedHashMap<>(body);
        toSign.put("accessKey", props.getAccessKey());
        body.put("signature", MomoSignature.forQuery(props.getSecretKey(), toSign));

        Map<String, Object> res = post(props.getQueryEndpoint(), body);
        return toResult(res);
    }

    private Map<String, Object> post(String url, Map<String, Object> body) {
        try {
            Map<String, Object> res = http.post().uri(url).contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON).body(body).retrieve().body(MAP_TYPE);
            if (res == null) throw new PaymentGatewayException("MoMo trả về phản hồi rỗng");
            return res;
        } catch (PaymentGatewayException e) {
            throw e;
        } catch (org.springframework.web.client.RestClientResponseException e) {
            String resp = e.getResponseBodyAsString();
            // Một số phản hồi MoMo trả HTTP 500 nhưng vẫn chứa JSON resultCode/message.
            // Đọc phản hồi có cấu trúc để service xử lý đúng mã lỗi, không mất nguyên nhân thật.
            try {
                Map<String, Object> errorBody = JSON.readValue(resp, new TypeReference<Map<String, Object>>() { });
                if (errorBody.get("resultCode") instanceof Number) {
                    Object code = errorBody.get("resultCode");
                    Object message = errorBody.get("message");
                    log.warn("MoMo trả HTTP {} resultCode={} message={}", e.getStatusCode().value(), code, message);
                    return errorBody;
                }
            } catch (Exception ignored) {
                // Không phải JSON MoMo hợp lệ; xử lý như lỗi HTTP thông thường.
            }
            log.error("MoMo trả HTTP {} cho {} (response body length={})", e.getStatusCode().value(), url, resp.length());
            throw new PaymentGatewayException("MoMo trả HTTP " + e.getStatusCode().value() + " nhưng không có resultCode hợp lệ", e);
        } catch (Exception e) {
            // Không đưa body/chữ ký vào log; chỉ ghi loại lỗi.
            log.error("Gọi MoMo thất bại ({}): {}", e.getClass().getSimpleName(), e.getMessage());
            throw new PaymentGatewayException("Không kết nối được cổng MoMo, vui lòng thử lại sau", e);
        }
    }

    private static MomoGatewayResult toResult(Map<String, Object> res) {
        Object code = res.get("resultCode");
        if (!(code instanceof Number)) throw new PaymentGatewayException("Phản hồi MoMo thiếu resultCode");
        return new MomoGatewayResult(((Number) code).intValue(), asLong(res.get("amount")), asLong(res.get("transId")),
                res.get("message") == null ? null : String.valueOf(res.get("message")),
                res.get("payUrl") == null ? null : String.valueOf(res.get("payUrl")));
    }

    private static Long asLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value instanceof String s && !s.isBlank()) {
            try { return Long.parseLong(s.trim()); } catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}
