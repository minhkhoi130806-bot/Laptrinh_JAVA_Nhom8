package com.restaurant.backend.payment;

/** Kết quả rút gọn từ API tạo giao dịch / truy vấn trạng thái của ZaloPay. */
public record ZalopayGatewayResult(
        int returnCode,
        String returnMessage,
        int subReturnCode,
        String subReturnMessage,
        String orderUrl,
        Long zpTransId,
        Long amount,
        Boolean isProcessing
) { }
