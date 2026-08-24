package com.eventhub.config;

/** Đọc cấu hình thanh toán từ biến môi trường của Tomcat/JVM. */
public final class PaymentConfig {
    private static final String DEFAULT_VNPAY_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    private PaymentConfig() {
    }

    public static String getProvider() {
        String value = System.getenv("PAYMENT_PROVIDER");
        if (value == null || value.isBlank()) return "MOCK";
        return "VNPAY".equalsIgnoreCase(value.trim()) ? "VNPAY" : "MOCK";
    }

    public static String getProviderLabel() {
        return "VNPAY".equals(getProvider()) ? "VNPAY Sandbox" : "Thanh toán mô phỏng";
    }

    public static int getHoldMinutes() {
        String value = System.getenv("PAYMENT_HOLD_MINUTES");
        if (value == null || value.isBlank()) return 15;
        try {
            return Math.max(5, Math.min(60, Integer.parseInt(value.trim())));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public static String getVnPayTmnCode() {
        return value("VNPAY_TMN_CODE");
    }

    public static String getVnPayHashSecret() {
        return value("VNPAY_HASH_SECRET");
    }

    public static String getVnPayPaymentUrl() {
        String value = value("VNPAY_PAYMENT_URL");
        return value == null ? DEFAULT_VNPAY_URL : value;
    }

    public static String getVnPayReturnUrl() {
        return value("VNPAY_RETURN_URL");
    }

    public static boolean isVnPayConfigured() {
        return getVnPayTmnCode() != null && getVnPayHashSecret() != null;
    }

    private static String value(String name) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
