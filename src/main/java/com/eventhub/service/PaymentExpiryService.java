package com.eventhub.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Tác vụ nền nhả các chỗ giữ quá hạn thanh toán. */
public final class PaymentExpiryService {
    private static ScheduledExecutorService executor;

    private PaymentExpiryService() {
    }

    public static synchronized void start() {
        if (executor != null && !executor.isShutdown()) return;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "eventhub-payment-expiry");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(() -> {
            try {
                int count = new PaymentService().expirePendingPayments();
                if (count > 0) {
                    System.out.println("[PaymentExpiry] Đã nhả " + count + " chỗ hết hạn.");
                }
            } catch (Exception e) {
                System.err.println("[PaymentExpiry] Lỗi: " + e.getMessage());
            }
        }, 30, 60, TimeUnit.SECONDS);
    }

    public static synchronized void shutdown() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
