package com.eventhub.payment;

import com.eventhub.config.PaymentConfig;
import com.eventhub.exception.PaymentException;
import com.eventhub.model.Payment;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/** Tạo URL và xác minh checksum theo giao thức VNPAY 2.1.0. */
public class VnPayPaymentGateway implements PaymentGateway {
    private static final DateTimeFormatter VNPAY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    public String getProviderCode() {
        return "VNPAY";
    }

    @Override
    public String createCheckoutUrl(Payment payment, HttpServletRequest request)
            throws PaymentException {
        if (!PaymentConfig.isVnPayConfigured()) {
            throw new PaymentException("Thiếu cấu hình VNPAY Sandbox.");
        }

        SortedMap<String, String> fields = new TreeMap<>();
        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", PaymentConfig.getVnPayTmnCode());
        fields.put("vnp_Amount", payment.getAmount()
                .multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString());
        fields.put("vnp_CurrCode", payment.getCurrency());
        fields.put("vnp_TxnRef", payment.getPaymentCode());
        fields.put("vnp_OrderInfo", "Thanh toan ve " + payment.getPaymentCode());
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_ReturnUrl", resolveReturnUrl(request));
        fields.put("vnp_IpAddr", getClientIp(request));
        fields.put("vnp_CreateDate", LocalDateTime.now().format(VNPAY_TIME));
        fields.put("vnp_ExpireDate", payment.getExpiresAt().format(VNPAY_TIME));

        String query = buildQuery(fields);
        String signature = hmacSha512(PaymentConfig.getVnPayHashSecret(), buildHashData(fields));
        return PaymentConfig.getVnPayPaymentUrl() + "?" + query +
                "&vnp_SecureHash=" + signature;
    }

    public boolean verifySignature(Map<String, String[]> parameterMap) {
        if (!PaymentConfig.isVnPayConfigured()) return false;
        String[] hashes = parameterMap.get("vnp_SecureHash");
        if (hashes == null || hashes.length == 0 || hashes[0] == null) return false;

        SortedMap<String, String> fields = new TreeMap<>();
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith("vnp_") || "vnp_SecureHash".equals(key)
                    || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            String[] values = entry.getValue();
            if (values != null && values.length > 0 && values[0] != null
                    && !values[0].isEmpty()) {
                fields.put(key, values[0]);
            }
        }

        String expected = hmacSha512(PaymentConfig.getVnPayHashSecret(), buildHashData(fields));
        return MessageDigest.isEqual(
                expected.toLowerCase().getBytes(StandardCharsets.US_ASCII),
                hashes[0].toLowerCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    private String resolveReturnUrl(HttpServletRequest request) {
        String configured = PaymentConfig.getVnPayReturnUrl();
        if (configured != null) return configured;

        String proto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String host = firstHeaderValue(request.getHeader("X-Forwarded-Host"));
        if (proto == null) proto = request.getScheme();
        if (host == null) {
            host = request.getServerName();
            int port = request.getServerPort();
            boolean defaultPort = ("http".equals(proto) && port == 80)
                    || ("https".equals(proto) && port == 443);
            if (!defaultPort) host += ":" + port;
        }
        return proto + "://" + host + request.getContextPath() + "/payment/vnpay/return";
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        String ip = forwarded == null ? request.getRemoteAddr() : forwarded;
        if (ip == null || ip.isBlank() || ip.contains(":")) return "127.0.0.1";
        return ip;
    }

    private String firstHeaderValue(String value) {
        if (value == null || value.isBlank()) return null;
        return value.split(",", 2)[0].trim();
    }

    private String buildQuery(SortedMap<String, String> fields) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (query.length() > 0) query.append('&');
            query.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return query.toString();
    }

    private String buildHashData(SortedMap<String, String> fields) {
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (data.length() > 0) data.append('&');
            data.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
        }
        return data.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String hmacSha512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Không thể ký dữ liệu VNPAY.", e);
        }
    }
}
