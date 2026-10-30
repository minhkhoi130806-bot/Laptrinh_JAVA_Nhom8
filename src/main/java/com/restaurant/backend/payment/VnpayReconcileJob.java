package com.restaurant.backend.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Đối soát định kỳ VNPay: hết hạn giao dịch treo và hoàn tất đơn đã nhận tiền nhưng chưa ghi hóa đơn. */
@Component
public class VnpayReconcileJob {
    private final VnpayPaymentService service;

    public VnpayReconcileJob(VnpayPaymentService service) { this.service = service; }

    @Scheduled(fixedDelay = 120_000, initialDelay = 60_000)
    public void run() { service.reconcile(); }
}
