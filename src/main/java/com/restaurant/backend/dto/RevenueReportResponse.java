package com.restaurant.backend.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * =========================================================================
 * DTO BÁO CÁO DOANH THU (RevenueReportResponse)
 * Người phụ trách: Ngô Minh Khôi (Tuần 4 - API Báo cáo Doanh thu)
 * 
 * Mục đích:
 * Đóng gói toàn bộ số liệu thống kê doanh thu gửi về cho giao diện Dashboard / Báo cáo:
 * - Tổng doanh thu toàn bộ hoặc trong khoảng thời gian
 * - Tổng số hóa đơn đã thanh toán
 * - Giá trị trung bình trên mỗi hóa đơn
 * - Doanh thu trong ngày hôm nay
 * - Chi tiết doanh thu theo từng ngày
 * - Chi tiết doanh thu theo từng phương thức thanh toán (Tiền mặt, Thẻ, Chuyển khoản...)
 * =========================================================================
 */
public record RevenueReportResponse(
        BigDecimal totalRevenue,
        Long totalInvoices,
        BigDecimal averageInvoiceValue,
        BigDecimal todayRevenue,
        List<DailyRevenueItem> dailyRevenues,
        List<PaymentMethodRevenueItem> paymentMethodRevenues
) {
    /**
     * DTO con: Doanh thu theo từng ngày
     */
    public record DailyRevenueItem(
            String date,
            BigDecimal revenue,
            Long invoiceCount
    ) {}

    /**
     * DTO con: Thống kê doanh thu theo phương thức thanh toán
     */
    public record PaymentMethodRevenueItem(
            String paymentMethod,
            BigDecimal revenue,
            Long invoiceCount
    ) {}
}

