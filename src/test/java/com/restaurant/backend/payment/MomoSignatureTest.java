package com.restaurant.backend.payment;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MomoSignatureTest {
    private static final String SECRET = "K951B6PE1waDMi640xX08PD3vg6EkVlz";

    @Test
    void createSignatureUsesAlphabeticalRawString() {
        Map<String, Object> v = new HashMap<>();
        v.put("accessKey", "F8BBA842ECF85"); v.put("amount", 50000L); v.put("extraData", "");
        v.put("ipnUrl", "https://example.com/ipn"); v.put("orderId", "HS1T1"); v.put("orderInfo", "pay");
        v.put("partnerCode", "MOMO"); v.put("redirectUrl", "https://example.com/r"); v.put("requestId", "HS1T1");
        v.put("requestType", "captureWallet");
        assertEquals("accessKey=F8BBA842ECF85&amount=50000&extraData=&ipnUrl=https://example.com/ipn&orderId=HS1T1&orderInfo=pay"
                + "&partnerCode=MOMO&redirectUrl=https://example.com/r&requestId=HS1T1&requestType=captureWallet",
                MomoSignature.raw(v, new String[]{"accessKey", "amount", "extraData", "ipnUrl", "orderId", "orderInfo",
                        "partnerCode", "redirectUrl", "requestId", "requestType"}));
        assertEquals(64, MomoSignature.forCreate(SECRET, v).length());
    }

    @Test
    void ipnSignatureDetectsTampering() {
        Map<String, Object> v = new HashMap<>();
        v.put("accessKey", "F8BBA842ECF85"); v.put("amount", 50000); v.put("extraData", ""); v.put("message", "Successful.");
        v.put("orderId", "HS1T1"); v.put("orderInfo", "pay"); v.put("orderType", "momo_wallet"); v.put("partnerCode", "MOMO");
        v.put("payType", "qr"); v.put("requestId", "HS1T1"); v.put("responseTime", 1700000000000L); v.put("resultCode", 0); v.put("transId", 4000000001L);
        String good = MomoSignature.forIpn(SECRET, v);
        assertTrue(MomoSignature.matches(good, good));
        v.put("amount", 1000); // kẻ tấn công sửa số tiền
        assertFalse(MomoSignature.matches(good, MomoSignature.forIpn(SECRET, v)));
        assertFalse(MomoSignature.matches(good, null));
    }
}
