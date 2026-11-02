package com.restaurant.backend.payment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZalopaySignatureTest {
    private static final String KEY1 = "sdngKKJmqEMzvh5QQcdD2A9XBSKUNaYn";
    private static final String KEY2 = "trMrHtvjo6myautxDUiAcYsVtaeQ8nhf";
    private static final int APP_ID = 2554;

    @Test
    void createSignatureMatchesHmacSha256() {
        String appTransId = "261102_HS1T1";
        String appUser = "0901234567";
        long amount = 50000L;
        long appTime = 1730000000000L;
        String embedData = "{\"redirecturl\":\"https://example.com/r\"}";
        String item = "[]";

        String signature = ZalopaySignature.forCreate(KEY1, APP_ID, appTransId, appUser, amount, appTime, embedData, item);
        assertNotNull(signature);
        assertEquals(64, signature.length()); // SHA-256 produces 64 hex characters

        // Chá»¯ kÃ½ pháº£i cÃ³ tÃ­nh táº¥t Ä‘á»‹nh (cÃ¹ng input sinh ra cÃ¹ng output)
        String signature2 = ZalopaySignature.forCreate(KEY1, APP_ID, appTransId, appUser, amount, appTime, embedData, item);
        assertTrue(ZalopaySignature.matches(signature, signature2));

        // Báº¥t ká»³ thay Ä‘á»•i nÃ o cÅ©ng lÃ m sai lá»‡ch chá»¯ kÃ½
        String tampered = ZalopaySignature.forCreate(KEY1, APP_ID, appTransId, appUser, 1000L, appTime, embedData, item);
        assertFalse(ZalopaySignature.matches(signature, tampered));
    }

    @Test
    void querySignatureGeneratesValidHash() {
        String appTransId = "261102_HS1T1";
        String signature = ZalopaySignature.forQuery(KEY1, APP_ID, appTransId);
        assertNotNull(signature);
        assertEquals(64, signature.length());
        assertTrue(ZalopaySignature.matches(signature, ZalopaySignature.forQuery(KEY1, APP_ID, appTransId)));
    }

    @Test
    void callbackSignatureDetectsTampering() {
        String validData = "{\"app_id\":2553,\"app_trans_id\":\"261102_HS1T1\",\"amount\":50000,\"zp_trans_id\":1234567890}";
        String goodMac = ZalopaySignature.forCallback(KEY2, validData);

        assertTrue(ZalopaySignature.verifyCallback(KEY2, validData, goodMac));
        assertTrue(ZalopaySignature.matches(goodMac, goodMac.toUpperCase())); // case-insensitive check

        // Káº» táº¥n cÃ´ng sá»­a ná»™i dung data
        String tamperedData = "{\"app_id\":2553,\"app_trans_id\":\"261102_HS1T1\",\"amount\":1000,\"zp_trans_id\":1234567890}";
        assertFalse(ZalopaySignature.verifyCallback(KEY2, tamperedData, goodMac));

        // Tham sá»‘ null hoáº·c sai key
        assertFalse(ZalopaySignature.verifyCallback(null, validData, goodMac));
        assertFalse(ZalopaySignature.verifyCallback("wrong_key", validData, goodMac));
        assertFalse(ZalopaySignature.verifyCallback(KEY2, validData, null));
    }
}
