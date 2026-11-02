package com.restaurant.backend.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Đối soát định kỳ ZaloPay: bù cho trường hợp callback không tới và đơn đã nhận tiền nhưng chưa ghi hóa đơn. */
@Component
public class ZalopayReconcileJob {
    private final ZalopayPaymentService service;

    public ZalopayReconcileJob(ZalopayPaymentService service) { this.service = service; }

    @Scheduled(fixedDelay = 120_000, initialDelay = 50_000)
    public void run() { service.reconcile(); }
}
