package com.eventhub.service;

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
 *
 * 3 chức năng chính:
 *   1. generateSummary  → Tóm tắt mô tả sự kiện (gemini-2.5-flash)
 *   2. generateImage    → Tạo ảnh banner (imagen-4-fast)
 *   3. chat             → Chatbot hỏi đáp (gemini-2.5-flash)
 */
public class GeminiService {

    // ===== HẰNG SỐ CẤU HÌNH =====
    private static final String API_KEY =
            System.getenv("GEMINI_API_KEY");

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // Model text: gemini-2.5-flash (500 RPD với Lite, đủ cho demo)
    private static final String TEXT_MODEL = "gemini-2.5-flash";

    // Model ảnh: imagen-4-fast (25 ảnh/ngày miễn phí)
    private static final String IMAGE_MODEL = "imagen-4-fast-generate-001";

    // Timeout 30 giây mỗi request
    private static final int TIMEOUT_SECONDS = 30;

    // Thư mục lưu ảnh AI generate (đọc từ env)
    private static final String UPLOAD_DIR =
            System.getenv("UPLOAD_BASE_DIR");

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
                    "Hãy tóm tắt mô tả sự kiện sau trong 2-3 câu ngắn gọn, " +
                            "súc tích và hấp dẫn bằng tiếng Việt. " +
                            "Chỉ trả về đoạn tóm tắt, không giải thích thêm.\n\n" +
                            "Tên sự kiện: %s\n" +
                            "Mô tả: %s",
                    title, description
            );

            // Build request body JSON
            String requestBody = buildTextRequestBody(prompt, 200, 0.7);

            // Gọi API
            String responseJson = callGeminiAPI(TEXT_MODEL + ":generateContent", requestBody);
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
        if (UPLOAD_DIR == null || UPLOAD_DIR.isBlank()) {
            System.err.println("[GeminiService] UPLOAD_BASE_DIR chưa cấu hình!");
            return null;
        }

        try {
            // Tạo prompt mô tả ảnh cần tạo
            String shortDesc = event.getDescription() != null
                    && event.getDescription().length() > 80
                    ? event.getDescription().substring(0, 80)
                    : event.getDescription();

            String prompt = String.format(
                    "Professional event banner image for an event titled '%s', " +
                            "category: '%s', description: '%s'. " +
                            "Clean modern design, suitable for student organization, " +
                            "vibrant colors, no text overlay, 16:9 landscape format.",
                    event.getTitle(),
                    event.getCategoryName() != null ? event.getCategoryName() : "Event",
                    shortDesc != null ? shortDesc : ""
            );

            // Build request body cho Imagen (khác với Text model)
            String requestBody = buildImageRequestBody(prompt);

            // Gọi Imagen API (endpoint khác: :predict thay vì :generateContent)
            String responseJson = callGeminiAPI(IMAGE_MODEL + ":predict", requestBody);
            if (responseJson == null) return null;

            // Lấy Base64 string từ response
            String base64Image = extractBase64FromResponse(responseJson);
            if (base64Image == null) return null;

            // Decode Base64 → byte[] → lưu file
            return saveBase64Image(base64Image);

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
            // Build request body với system_instruction + contents array
            String requestBody = buildChatRequestBody(systemPrompt, history);

            // Gọi API
            String responseJson = callGeminiAPI(TEXT_MODEL + ":generateContent", requestBody);
            if (responseJson == null) {
                return "Xin lỗi, tôi không thể trả lời lúc này. Vui lòng thử lại!";
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
     *
     * Format Gemini generateContent:
     * {
     *   "contents": [{"parts": [{"text": "..."}]}],
     *   "generationConfig": {"maxOutputTokens": ..., "temperature": ...}
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
     * Build JSON body cho request tạo ảnh (Imagen).
     *
     * Format Imagen predict:
     * {
     *   "instances": [{"prompt": "..."}],
     *   "parameters": {"sampleCount": 1, "aspectRatio": "16:9"}
     * }
     */
    private String buildImageRequestBody(String prompt) {
        JsonObject root = new JsonObject();

        // instances array
        JsonArray instances = new JsonArray();
        JsonObject instance = new JsonObject();
        instance.addProperty("prompt", prompt);
        instances.add(instance);
        root.add("instances", instances);

        // parameters
        JsonObject params = new JsonObject();
        params.addProperty("sampleCount", 1);
        params.addProperty("aspectRatio", "16:9");
        root.add("parameters", params);

        return GSON.toJson(root);
    }

    /**
     * Build JSON body cho chatbot multi-turn.
     *
     * Format:
     * {
     *   "system_instruction": {"parts": [{"text": "...system prompt..."}]},
     *   "contents": [
     *     {"role": "user",  "parts": [{"text": "tin nhắn 1"}]},
     *     {"role": "model", "parts": [{"text": "trả lời 1"}]},
     *     ...
     *   ],
     *   "generationConfig": {...}
     * }
     *
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
        genConfig.addProperty("maxOutputTokens", 500);
        genConfig.addProperty("temperature", 0.8);
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
    private String callGeminiAPI(String modelAndAction, String requestBody) {
        try {
            // Build URL: BASE_URL + model:action + ?key=API_KEY
            String url = BASE_URL + modelAndAction + "?key=" + API_KEY;

            // Tạo HTTP request
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                    .build();

            // Gửi request và nhận response
            HttpResponse<String> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            // Kiểm tra HTTP status code
            if (response.statusCode() != 200) {
                System.err.println("[GeminiService] API trả về lỗi: "
                        + response.statusCode()
                        + " | " + response.body().substring(0,
                        Math.min(200, response.body().length())));
                return null;
            }

            return response.body();

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi gọi API: " + e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất text từ Gemini generateContent response.
     *
     * Cấu trúc JSON response:
     * {
     *   "candidates": [{
     *     "content": {
     *       "parts": [{"text": "KẾT QUẢ Ở ĐÂY"}]
     *     }
     *   }]
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

            String text = parts.get(0).getAsJsonObject()
                    .get("text").getAsString();

            return text.trim();

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi parse text response: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Trích xuất Base64 image từ Imagen response.
     *
     * Cấu trúc JSON response Imagen:
     * {
     *   "predictions": [{
     *     "bytesBase64Encoded": "BASE64_STRING_Ở_ĐÂY"
     *   }]
     * }
     */
    private String extractBase64FromResponse(String responseJson) {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();

            JsonArray predictions = json.getAsJsonArray("predictions");
            if (predictions == null || predictions.isEmpty()) return null;

            JsonObject prediction = predictions.get(0).getAsJsonObject();
            JsonElement base64Element = prediction.get("bytesBase64Encoded");
            if (base64Element == null) return null;

            return base64Element.getAsString();

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi parse image response: "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Decode Base64 string thành file ảnh JPG và lưu vào server.
     *
     * @param base64Data Chuỗi Base64
     * @return Tên file đã lưu (ví dụ: "ai_abc123.jpg"), null nếu lỗi
     */
    private String saveBase64Image(String base64Data) {
        try {
            // Decode Base64 → byte array
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // Tạo tên file ngẫu nhiên (UUID) để tránh trùng
            String fileName = "ai_" + UUID.randomUUID().toString() + ".jpg";

            // Đường dẫn thư mục lưu
            Path uploadPath = Paths.get(UPLOAD_DIR, "events");

            // Tạo thư mục nếu chưa có
            Files.createDirectories(uploadPath);

            // Lưu file
            Path filePath = uploadPath.resolve(fileName);
            Files.write(filePath, imageBytes);

            System.out.println("[GeminiService] Đã lưu ảnh AI: " + fileName);
            return fileName;

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi lưu ảnh: " + e.getMessage());
            return null;
        }
    }
}