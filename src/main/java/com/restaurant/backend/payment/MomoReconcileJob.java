package com.restaurant.backend.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Đối soát định kỳ: bù cho trường hợp IPN không tới (máy local, mạng lỗi) và đơn đã nhận tiền nhưng chưa ghi hóa đơn. */
@Component
public class MomoReconcileJob {
    private final MomoPaymentService service;

    public MomoReconcileJob(MomoPaymentService service) { this.service = service; }

    @Scheduled(fixedDelay = 120_000, initialDelay = 45_000)
    public void run() { service.reconcile(); }
}
