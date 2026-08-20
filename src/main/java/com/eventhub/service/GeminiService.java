package com.eventhub.service;

import com.eventhub.config.UploadConfig;
import com.eventhub.model.Event;
import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;

/**
 * Service gọi Google Gemini API.
 * Dùng java.net.http.HttpClient (built-in Java 11+), không cần SDK ngoài.
 * <p>
 * 3 chức năng chính:
 * 1. generateSummary  → tóm tắt sự kiện (gemini-3.6-flash)
 * 2. generateImage    → banner 16:9 (gemini-3.1-flash-image / generateContent)
 * 3. chat             → chatbot (gemini-3.6-flash)
 */
public class GeminiService {

    // ===== HẰNG SỐ CẤU HÌNH =====
    private static final String API_KEY =
            System.getenv("GEMINI_API_KEY");

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String[] TEXT_MODELS = {
            "gemini-3.6-flash",
            "gemini-2.5-flash",
            "gemini-2.0-flash"
    };

    private static final String[] IMAGE_MODELS = {
            "gemini-3.1-flash-image",
            "gemini-2.5-flash-image",
            "gemini-2.0-flash-preview-image-generation"
    };

    private static final int TIMEOUT_SECONDS = 30;
    private static final int IMAGE_TIMEOUT_SECONDS = 90;

    // HttpClient dùng chung (thread-safe, tạo 1 lần)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
            .build();

    // Gson để parse/build JSON
    private static final Gson GSON = new GsonBuilder().create();

    // =====================================================
    // PHƯƠNG THỨC 1: TÓM TẮT MÔ TẢ SỰ KIỆN
    // =====================================================

    /**
     * Gọi Gemini để tóm tắt mô tả sự kiện thành 2-3 câu ngắn gọn.
     *
     * @param title       Tên sự kiện
     * @param description Mô tả đầy đủ
     * @return Chuỗi tóm tắt, hoặc null nếu lỗi (không throw exception)
     */
    public String generateSummary(String title, String description) {
        // Kiểm tra API key đã cấu hình chưa
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("[GeminiService] GEMINI_API_KEY chưa được cấu hình!");
            return null;
        }

        try {
            // Tạo prompt yêu cầu Gemini tóm tắt
            String prompt = String.format(
                    "Bạn là copywriter sự kiện dành cho sinh viên Việt Nam.\n" +
                            "Viết MỘT đoạn tóm tắt 2 câu (tối đa 180 ký tự) cho sự kiện dưới đây.\n" +
                            "Yêu cầu:\n" +
                            "- Câu 1: nêu rõ đây là sự kiện gì và dành cho ai\n" +
                            "- Câu 2: nêu 1 lợi ích / điểm đặc biệt để người đọc muốn đăng ký\n" +
                            "- Giọng thân thiện, cụ thể, không sáo rỗng\n" +
                            "- Không dùng ngoặc kép, không tiền tố như 'Tóm tắt:', không emoji\n" +
                            "- Chỉ trả về đoạn tóm tắt\n\n" +
                            "Tên sự kiện: %s\n" +
                            "Mô tả: %s",
                    title,
                    description != null && description.length() > 800
                            ? description.substring(0, 800)
                            : description
            );

            String requestBody = buildTextRequestBody(prompt, 400, 0.4);
            String responseJson = generateContent(TEXT_MODELS, requestBody, TIMEOUT_SECONDS);
            if (responseJson == null) return null;

            // Parse kết quả từ JSON response
            return extractTextFromResponse(responseJson);

        } catch (Exception e) {
            // KHÔNG throw exception ra ngoài → AI thất bại không crash luồng chính
            System.err.println("[GeminiService] Lỗi generateSummary: " + e.getMessage());
            return null;
        }
    }

    // =====================================================
    // PHƯƠNG THỨC 2: TẠO ẢNH BANNER (IMAGEN 4)
    // =====================================================

    /**
     * Gọi Imagen 4 để tạo ảnh banner tự động cho sự kiện.
     * Lưu ảnh vào thư mục uploads/events/ và trả về tên file.
     *
     * @param event Sự kiện cần tạo ảnh
     * @return Tên file ảnh (ví dụ: "ai_abc123.jpg"), hoặc null nếu lỗi
     */
    public String generateEventImage(Event event) {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("[GeminiService] GEMINI_API_KEY chưa cấu hình!");
            return null;
        }
        try {
            String desc = event.getDescription() != null ? event.getDescription() : "";
            if (desc.length() > 400) {
                desc = desc.substring(0, 400);
            }

            String prompt = String.format(
                    "Generate a photorealistic 16:9 event banner photograph. " +
                            "No text, no letters, no logos, no watermarks, no UI mockup.\n" +
                            "Event title: %s\n" +
                            "Category: %s\n" +
                            "Setting and details: %s\n" +
                            "Style: vibrant campus/student event, cinematic lighting, " +
                            "sharp focus, real people and venue atmosphere matching the category.",
                    event.getTitle(),
                    event.getCategoryName() != null ? event.getCategoryName() : "student event",
                    desc
            );

            String requestBody = buildImageRequestBody(prompt);
            String responseJson = generateContent(
                    IMAGE_MODELS,
                    requestBody,
                    IMAGE_TIMEOUT_SECONDS
            );
            if (responseJson == null) return null;

            InlineImage image = extractInlineImage(responseJson);
            if (image == null) return null;

            return saveBase64Image(image.data(), image.mimeType());

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi generateEventImage: " + e.getMessage());
            return null;
        }
    }

    // =====================================================
    // PHƯƠNG THỨC 3: CHATBOT (MULTI-TURN)
    // =====================================================

    /**
     * Gọi Gemini để xử lý 1 lượt hội thoại chatbot.
     * Gửi kèm toàn bộ lịch sử chat để Gemini hiểu context.
     *
     * @param systemPrompt Hướng dẫn vai trò + context sự kiện
     * @param history      Lịch sử chat (list các map {role, content})
     * @return Câu trả lời của AI, hoặc message fallback nếu lỗi
     */
    public String chat(String systemPrompt,
                       List<Map<String, String>> history) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "Xin lỗi, chatbot hiện không khả dụng. Vui lòng thử lại sau.";
        }

        try {
            String requestBody = buildChatRequestBody(systemPrompt, history);
            String responseJson = generateContent(TEXT_MODELS, requestBody, TIMEOUT_SECONDS);
            if (responseJson == null) {
                return "Hệ thống AI đang quá tải (Google 503). Bạn thử lại sau vài giây nhé!";
            }

            // Lấy text từ response
            String reply = extractTextFromResponse(responseJson);
            return reply != null ? reply
                    : "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại!";

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi chat: " + e.getMessage());
            return "Xin lỗi, tôi gặp sự cố kỹ thuật. Vui lòng thử lại sau!";
        }
    }

    // =====================================================
    // PRIVATE HELPERS — BUILD REQUEST BODY
    // =====================================================

    /**
     * Build JSON body cho request tạo text (summary).
     * <p>
     * Format Gemini generateContent:
     * {
     * "contents": [{"parts": [{"text": "..."}]}],
     * "generationConfig": {"maxOutputTokens": ..., "temperature": ...}
     * }
     */
    private String buildTextRequestBody(String prompt,
                                        int maxTokens,
                                        double temperature) {
        // Dùng JsonObject của Gson để build JSON an toàn (tránh lỗi escape)
        JsonObject root = new JsonObject();

        // contents array
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);

        content.add("parts", parts);
        contents.add(content);
        root.add("contents", contents);

        // generationConfig
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", maxTokens);
        genConfig.addProperty("temperature", temperature);
        root.add("generationConfig", genConfig);

        return GSON.toJson(root);
    }

    /**
     * Build JSON body cho Gemini native image (generateContent).
     */
    private String buildImageRequestBody(String prompt) {
        JsonObject root = new JsonObject();

        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        content.addProperty("role", "user");

        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        root.add("contents", contents);

        JsonObject genConfig = new JsonObject();
        JsonArray modalities = new JsonArray();
        modalities.add("TEXT");
        modalities.add("IMAGE");
        genConfig.add("responseModalities", modalities);

        JsonObject imageConfig = new JsonObject();
        imageConfig.addProperty("aspectRatio", "16:9");
        genConfig.add("imageConfig", imageConfig);

        root.add("generationConfig", genConfig);
        return GSON.toJson(root);
    }

    /**
     * Build JSON body cho chatbot multi-turn.
     * <p>
     * Format:
     * {
     * "system_instruction": {"parts": [{"text": "...system prompt..."}]},
     * "contents": [
     * {"role": "user",  "parts": [{"text": "tin nhắn 1"}]},
     * {"role": "model", "parts": [{"text": "trả lời 1"}]},
     * ...
     * ],
     * "generationConfig": {...}
     * }
     * <p>
     * LƯU Ý: Gemini dùng "model" thay vì "assistant"!
     */
    private String buildChatRequestBody(String systemPrompt,
                                        List<Map<String, String>> history) {
        JsonObject root = new JsonObject();

        // system_instruction (khác với OpenAI format)
        JsonObject sysInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        sysInstruction.add("parts", sysParts);
        root.add("system_instruction", sysInstruction);

        // contents array = toàn bộ lịch sử chat
        JsonArray contents = new JsonArray();
        for (Map<String, String> msg : history) {
            JsonObject contentObj = new JsonObject();

            // "role" trong Gemini: "user" hoặc "model"
            // Nếu history lưu "assistant" thì đổi sang "model"
            String role = msg.get("role");
            if ("assistant".equals(role)) role = "model";
            contentObj.addProperty("role", role);

            JsonArray parts = new JsonArray();
            JsonObject partObj = new JsonObject();
            partObj.addProperty("text", msg.get("content"));
            parts.add(partObj);
            contentObj.add("parts", parts);

            contents.add(contentObj);
        }
        root.add("contents", contents);

        // generationConfig
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", 1024);
        genConfig.addProperty("temperature", 0.35);
        genConfig.addProperty("topP", 0.9);
        root.add("generationConfig", genConfig);

        return GSON.toJson(root);
    }

    // =====================================================
    // PRIVATE HELPERS — CALL API & PARSE RESPONSE
    // =====================================================

    /**
     * Gửi HTTP POST request đến Gemini API.
     *
     * @param modelAndAction Ví dụ: "gemini-2.5-flash:generateContent"
     * @param requestBody    JSON string
     * @return Response JSON string, hoặc null nếu lỗi
     */
    private String generateContent(String[] models, String requestBody, int timeoutSeconds) {
        for (String model : models) {
            GeminiResponse result = callGeminiAPI(model + ":generateContent", requestBody, timeoutSeconds);
            if (result.ok()) {
                if (!model.equals(models[0])) {
                    System.out.println("[GeminiService] Dung model du phong: " + model);
                }
                return result.body();
            }
            if (result.quotaExceeded()) {
                System.err.println("[GeminiService] Het quota/rate limit — dung goi AI.");
                return null;
            }
        }
        return null;
    }

    private GeminiResponse callGeminiAPI(String modelAndAction, String requestBody, int timeoutSeconds) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String url = BASE_URL + modelAndAction + "?key=" + API_KEY;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(timeoutSeconds))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                int status = response.statusCode();
                if (status == 200) {
                    return GeminiResponse.ok(response.body());
                }

                String body = response.body() != null ? response.body() : "";
                System.err.println("[GeminiService] API tra ve loi: "
                        + status
                        + " (" + modelAndAction + ") | "
                        + body.substring(0, Math.min(220, body.length())));

                if (status == 429) {
                    return GeminiResponse.quota();
                }
                boolean retryable = status == 502 || status == 503;
                if (!retryable) {
                    return GeminiResponse.fail();
                }
                if (attempt < 3) {
                    Thread.sleep(600L * attempt);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return GeminiResponse.fail();
            } catch (Exception e) {
                System.err.println("[GeminiService] Loi goi API: " + e.getMessage());
                if (attempt < 3) {
                    try {
                        Thread.sleep(600L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return GeminiResponse.fail();
                    }
                }
            }
        }
        return GeminiResponse.fail();
    }

    private record GeminiResponse(String body, boolean quotaExceeded) {
        static GeminiResponse ok(String body) {
            return new GeminiResponse(body, false);
        }

        static GeminiResponse quota() {
            return new GeminiResponse(null, true);
        }

        static GeminiResponse fail() {
            return new GeminiResponse(null, false);
        }

        boolean ok() {
            return body != null && !quotaExceeded;
        }
    }

    /**
     * Trích xuất text từ Gemini generateContent response.
     * <p>
     * Cấu trúc JSON response:
     * {
     * "candidates": [{
     * "content": {
     * "parts": [{"text": "KẾT QUẢ Ở ĐÂY"}]
     * }
     * }]
     * }
     */
    private String extractTextFromResponse(String responseJson) {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();

            // Đi sâu vào cấu trúc: candidates[0].content.parts[0].text
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            JsonObject content = candidate.getAsJsonObject("content");
            if (content == null) return null;

            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null || parts.isEmpty()) return null;

            StringBuilder text = new StringBuilder();
            for (JsonElement partEl : parts) {
                JsonObject part = partEl.getAsJsonObject();
                if (part.has("text") && !part.get("text").isJsonNull()) {
                    text.append(part.get("text").getAsString());
                }
            }
            String result = text.toString().trim();
            return result.isEmpty() ? null : result;

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi parse text response: "
                    + e.getMessage());
            return null;
        }
    }

    private record InlineImage(String data, String mimeType) {
    }

    /**
     * Lấy ảnh từ generateContent: candidates[0].content.parts[].inlineData
     */
    private InlineImage extractInlineImage(String responseJson) {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
                System.err.println("[GeminiService] Image response không có candidates");
                return null;
            }

            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            if (content == null) return null;

            JsonArray parts = content.getAsJsonArray("parts");
            if (parts == null) return null;

            for (JsonElement partEl : parts) {
                JsonObject part = partEl.getAsJsonObject();
                if (!part.has("inlineData") || !part.get("inlineData").isJsonObject()) {
                    continue;
                }
                JsonObject inline = part.getAsJsonObject("inlineData");
                if (inline.has("data") && !inline.get("data").isJsonNull()) {
                    String mime = inline.has("mimeType")
                            ? inline.get("mimeType").getAsString()
                            : "image/png";
                    return new InlineImage(inline.get("data").getAsString(), mime);
                }
            }

            System.err.println("[GeminiService] Image response không có inlineData");
            return null;
        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi parse image response: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decode Base64 thành file ảnh và lưu vào uploads/events.
     */
    private String saveBase64Image(String base64Data, String mimeType) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String extension = extensionFromMime(mimeType);
            String fileName = "ai_" + UUID.randomUUID() + "." + extension;

            Path uploadPath = UploadConfig.getBaseDir().resolve("events");
            Files.createDirectories(uploadPath);
            Files.write(uploadPath.resolve(fileName), imageBytes);

            System.out.println("[GeminiService] Da luu anh AI: " + fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi lưu ảnh: " + e.getMessage());
            return null;
        }
    }

    private static String extensionFromMime(String mimeType) {
        if (mimeType == null) return "png";
        String mime = mimeType.toLowerCase();
        if (mime.contains("jpeg") || mime.contains("jpg")) return "jpg";
        if (mime.contains("webp")) return "webp";
        return "png";
    }
}