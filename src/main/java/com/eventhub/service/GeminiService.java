package com.eventhub.service;

import com.eventhub.config.UploadConfig;
import com.eventhub.model.Event;
import com.google.gson.*;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Service gọi Google Gemini API và hỗ trợ AI Image Generation.
 * Dùng java.net.http.HttpClient (built-in Java 11+).
 * <p>
 * 3 chức năng chính:
 * 1. generateSummary  → tóm tắt sự kiện (gemini-3.6-flash)
 * 2. generateEventImage → banner 16:9 chất lượng cao, đúng chủ đề
 * 3. chat             → chatbot tư vấn sự kiện (gemini-3.6-flash)
 */
public class GeminiService {

    // ===== HẰNG SỐ CẤU HÌNH =====
    private static final String API_KEY =
            System.getenv("GEMINI_API_KEY");

    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    // Có thể ghi đè danh sách model bằng GEMINI_TEXT_MODELS (phân tách bằng dấu phẩy)
    // để đổi model mà không phải build lại ứng dụng.
    private static final String[] TEXT_MODELS = loadTextModels();

    // Timeout kết nối ngắn, nhưng cho phép model đủ thời gian sinh câu trả lời dài.
    // GEMINI_CHAT_TIMEOUT_SECONDS có thể đặt trong khoảng 15-180 giây.
    private static final int CONNECT_TIMEOUT_SECONDS = 10;
    private static final int SUMMARY_TIMEOUT_SECONDS = 45;
    private static final int CHAT_TIMEOUT_SECONDS = readIntEnv(
            "GEMINI_CHAT_TIMEOUT_SECONDS", 90, 15, 180);
    private static final int IMAGE_TIMEOUT_SECONDS = 60;
    private static final int CHAT_MAX_OUTPUT_TOKENS = 4096;

    // HttpClient dùng chung (thread-safe)
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    // Gson để parse/build JSON
    private static final Gson GSON = new GsonBuilder().create();

    private static String[] loadTextModels() {
        String configured = System.getenv("GEMINI_TEXT_MODELS");
        if (configured != null && !configured.isBlank()) {
            String[] models = Arrays.stream(configured.split(","))
                    .map(String::trim)
                    .filter(model -> !model.isBlank())
                    .toArray(String[]::new);
            if (models.length > 0) {
                return models;
            }
        }

        // Ưu tiên model flash nhanh, giữ các model ổn định làm fallback.
        return new String[]{
                "gemini-3.6-flash",
                "gemini-3.5-flash",
                "gemini-3.0-flash",
                "gemini-2.5-flash"
        };
    }

    private static int readIntEnv(String name, int defaultValue,
                                  int min, int max) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return Math.max(min, Math.min(max, parsed));
        } catch (NumberFormatException e) {
            System.err.println("[GeminiService] Giá trị " + name
                    + " không hợp lệ, dùng mặc định " + defaultValue + "s.");
            return defaultValue;
        }
    }

    // =====================================================
    // PHƯƠNG THỨC 1: TÓM TẮT MÔ TẢ SỰ KIỆN
    // =====================================================

    /**
     * Gọi Gemini để tóm tắt mô tả sự kiện thành 2 câu ngắn gọn.
     *
     * @param title       Tên sự kiện
     * @param description Mô tả đầy đủ
     * @return Chuỗi tóm tắt, hoặc null nếu lỗi
     */
    public String generateSummary(String title, String description) {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("[GeminiService] GEMINI_API_KEY chưa được cấu hình!");
            return null;
        }

        try {
            String prompt = String.format(
                    "Bạn là copywriter sự kiện dành cho sinh viên Việt Nam.\n" +
                            "Viết MỘT đoạn tóm tắt ngắn gọn đúng 2 câu (khoảng 120-180 ký tự) cho sự kiện dưới đây.\n" +
                            "Yêu cầu:\n" +
                            "- Câu 1: nêu rõ đây là sự kiện gì và dành cho ai\n" +
                            "- Câu 2: nêu 1 lợi ích / điểm hấp dẫn nhất để người đọc muốn đăng ký ngay\n" +
                            "- Giọng văn thân thiện, cuốn hút, súc tích\n" +
                            "- Không dùng ngoặc kép, không tiền tố như 'Tóm tắt:', không emoji\n" +
                            "- Chỉ trả về duy nhất đoạn văn tóm tắt\n\n" +
                            "Tên sự kiện: %s\n" +
                            "Mô tả: %s",
                    title,
                    description != null && description.length() > 800
                            ? description.substring(0, 800)
                            : description
            );

            String requestBody = buildTextRequestBody(prompt, 500, 0.4);
            String responseJson = generateContent(TEXT_MODELS, requestBody, SUMMARY_TIMEOUT_SECONDS);
            if (responseJson == null) return null;

            return extractTextFromResponse(responseJson);

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi generateSummary: " + e.getMessage());
            return null;
        }
    }

    // =====================================================
    // PHƯƠNG THỨC 2: TẠO ẢNH BANNER SỰ KIỆN (16:9)
    // =====================================================

    /**
     * Tạo ảnh banner 16:9 đúng chủ đề cho sự kiện.
     * Thứ tự ưu tiên:
     * 1. Google Imagen 3 (nếu API key có quota)
     * 2. Gemini 2.0 native image generation
     * 3. AI Image Generator Fallback (Pollinations AI miễn phí, không bị giới hạn 429)
     *
     * @param event Sự kiện cần tạo ảnh
     * @return Tên file ảnh đã lưu (vd: "ai_abc123.jpg"), hoặc null nếu thất bại
     */
    public String generateEventImage(Event event) {
        String prompt = buildSmartImagePrompt(event);

        // 1. Thử qua Google Imagen 3 / Gemini Image nếu có API Key
        if (API_KEY != null && !API_KEY.isBlank()) {
            String imagenFile = tryImagen3Predict(prompt);
            if (imagenFile != null) {
                return imagenFile;
            }

            String geminiFile = tryGeminiNativeImage(prompt);
            if (geminiFile != null) {
                return geminiFile;
            }
        }

        // 2. Tự động Fallback sang AI Image Generator miễn phí (Pollinations AI)
        return tryFreeAiImage(prompt);
    }

    /**
     * Tạo prompt tiếng Anh chuyên biệt, giàu chi tiết cho từng loại sự kiện
     * (tránh tình trạng sinh ra ảnh sân vận động tối tăm, lạc đề).
     */
    private String buildSmartImagePrompt(Event event) {
        String title = event.getTitle() != null ? event.getTitle() : "";
        String category = event.getCategoryName() != null ? event.getCategoryName() : "";
        String desc = event.getDescription() != null ? event.getDescription() : "";
        String combined = (title + " " + category + " " + desc).toLowerCase();

        String scene;

        if (containsAny(combined, "figma", "ui/ux", "ui-ux", "ux", "ui design", "thiết kế giao diện", "thiết kế đồ họa", "design sprint")) {
            scene = "creative university students in a bright modern UI/UX design studio workshop, collaborating on laptops showing colorful Figma interface mockups, wireframes, sticky notes on glass wall, warm natural daylight, clean aesthetic";
        } else if (containsAny(combined, "ai", "trí tuệ nhân tạo", "gemini", "chatgpt", "machine learning", "deep learning", "data", "pandas", "python", "dữ liệu")) {
            scene = "modern university computer lab workshop, students coding and analyzing data visualizations and AI neural network charts on bright screens, high-tech modern classroom, bright clean daytime lighting";
        } else if (containsAny(combined, "hackathon", "git", "github", "lập trình", "coding", "flutter", "mobile app", "blockchain", "web3", "web dev")) {
            scene = "enthusiastic university students coding at a software development hackathon, laptops with code editors and app prototypes on desks, modern campus innovation hub, bright dynamic tech atmosphere";
        } else if (containsAny(combined, "music", "acoustic", "âm nhạc", "văn nghệ", "ca nhạc", "hát")) {
            scene = "cozy campus acoustic music gathering, university students performing acoustic guitar on small warm wooden deck decorated with fairy string lights, happy student audience, intimate golden glow";
        } else if (containsAny(combined, "xanh", "green", "môi trường", "tình nguyện", "ngoại khóa", "teambuilding", "trồng cây", "dọn vệ sinh")) {
            scene = "cheerful university students in outdoor volunteer team activity on green sunny university campus, planting saplings, smiling young people working together, sunny blue sky, lush green grass and trees";
        } else if (containsAny(combined, "khởi nghiệp", "startup", "founder", "pitching", "pitch", "gọi vốn")) {
            scene = "young student entrepreneurs presenting startup pitch deck in modern innovation co-working space, whiteboard brainstorming, bright natural daylight, creative professional atmosphere";
        } else if (containsAny(combined, "phỏng vấn", "cv", "tuyển dụng", "career", "việc làm", "interview")) {
            scene = "university career coaching fair, students in smart casual attire practicing job interviews with mentors, resume reviews in bright professional hall, friendly warm atmosphere";
        } else if (containsAny(combined, "tiếng anh", "english", "presentation", "thuyết trình", "clb")) {
            scene = "students active group speaking club meeting, standing in bright modern lounge practicing public presentation, smiling, engaged lively discussion";
        } else if (containsAny(combined, "hội thảo", "seminar", "talkshow", "hội nghị", "diễn giả")) {
            scene = "professional academic seminar in an elegant university conference hall, speaker presenting on stage with projector screen, modern auditorium with attentive student audience, warm bright lighting";
        } else if (containsAny(combined, "workshop", "thực hành", "khóa học", "lab")) {
            scene = "interactive university workshop session in a bright modern classroom, enthusiastic students sitting at tables with laptops and notebooks, hands-on learning, warm pleasant lighting";
        } else if (containsAny(combined, "cuộc thi", "contest", "quiz", "thi đấu")) {
            scene = "thrilling university student innovation competition, teams presenting projects with prototype displays, judges, energetic academic tournament atmosphere, bright dynamic lighting";
        } else {
            scene = "vibrant university campus student event, modern college setting, happy young people collaborating, bright natural lighting";
        }

        return String.format(
                "Photorealistic 16:9 banner photograph of %s. High quality 8k resolution, sharp focus, natural vivid lighting, real campus atmosphere, no text, no letters, no logos, no typography, no watermark, no dark empty stadium, no concert arena.",
                scene
        );
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            String regex = "(^|[^a-zA-Z0-9_àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ])"
                    + Pattern.quote(kw)
                    + "([^a-zA-Z0-9_àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ]|$)";
            if (Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS).matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    // =====================================================
    // PHƯƠNG THỨC 3: CHATBOT (MULTI-TURN)
    // =====================================================

    /**
     * Gọi Gemini để xử lý lượt hội thoại chatbot.
     *
     * @param systemPrompt Hướng dẫn vai trò + context sự kiện
     * @param history      Lịch sử chat
     * @return Câu trả lời của AI
     */
    public String chat(String systemPrompt,
                       List<Map<String, String>> history) {
        if (API_KEY == null || API_KEY.isBlank()) {
            return "Xin lỗi, hệ thống AI chưa được cấu hình khóa API. Vui lòng liên hệ quản trị viên!";
        }

        try {
            String requestBody = buildChatRequestBody(systemPrompt, history);
            String responseJson = generateContent(TEXT_MODELS, requestBody, CHAT_TIMEOUT_SECONDS);
            if (responseJson == null) {
                return "Hệ thống AI phản hồi quá lâu hoặc đang quá tải. Bạn vui lòng thử lại sau vài giây nhé!";
            }

            String reply = extractTextFromResponse(responseJson);
            return reply != null ? reply
                    : "Xin lỗi, tôi chưa xử lý được câu hỏi. Bạn vui lòng thử lại nhé!";

        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi chat: " + e.getMessage());
            return "Xin lỗi, tôi gặp sự cố kết nối. Bạn vui lòng thử lại sau giây lát!";
        }
    }

    // =====================================================
    // PRIVATE HELPERS — BUILD REQUEST BODY
    // =====================================================

    private String buildTextRequestBody(String prompt,
                                        int maxTokens,
                                        double temperature) {
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
        genConfig.addProperty("maxOutputTokens", maxTokens);
        genConfig.addProperty("temperature", temperature);
        root.add("generationConfig", genConfig);

        return GSON.toJson(root);
    }

    private String buildNativeImageRequestBody(String prompt) {
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
        modalities.add("IMAGE");
        genConfig.add("responseModalities", modalities);

        JsonObject imageConfig = new JsonObject();
        imageConfig.addProperty("aspectRatio", "16:9");
        genConfig.add("imageConfig", imageConfig);

        root.add("generationConfig", genConfig);
        return GSON.toJson(root);
    }

    private String buildChatRequestBody(String systemPrompt,
                                        List<Map<String, String>> history) {
        JsonObject root = new JsonObject();

        // system_instruction
        JsonObject sysInstruction = new JsonObject();
        JsonArray sysParts = new JsonArray();
        JsonObject sysPart = new JsonObject();
        sysPart.addProperty("text", systemPrompt);
        sysParts.add(sysPart);
        sysInstruction.add("parts", sysParts);
        root.add("system_instruction", sysInstruction);

        // contents array
        JsonArray contents = new JsonArray();
        for (Map<String, String> msg : history) {
            JsonObject contentObj = new JsonObject();

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

        // generationConfig: tăng giới hạn để câu trả lời dài không bị cắt giữa chừng
        JsonObject genConfig = new JsonObject();
        genConfig.addProperty("maxOutputTokens", CHAT_MAX_OUTPUT_TOKENS);
        genConfig.addProperty("temperature", 0.35);
        genConfig.addProperty("topP", 0.9);
        root.add("generationConfig", genConfig);

        return GSON.toJson(root);
    }

    // =====================================================
    // PRIVATE HELPERS — CALL API & FALLBACKS
    // =====================================================

    private String generateContent(String[] models, String requestBody, int timeoutSeconds) {
        boolean timeoutAlreadySeen = false;

        for (String model : models) {
            GeminiResponse result = callGeminiAPI(
                    model + ":generateContent", requestBody, timeoutSeconds);
            if (result.ok()) {
                if (!model.equals(models[0])) {
                    System.out.println("[GeminiService] Sử dụng model phụ: " + model);
                }
                return result.body();
            }
            if (result.quotaExceeded()) {
                System.err.println("[GeminiService] Model " + model
                        + " hết quota (429) — đang thử model tiếp theo...");
                continue;
            }
            if (result.timedOut()) {
                // Một lần fallback là đủ. Nếu tiếp tục thử toàn bộ model sau
                // timeout, một request có thể bị treo nhiều phút.
                if (timeoutAlreadySeen) {
                    break;
                }
                timeoutAlreadySeen = true;
                System.err.println("[GeminiService] Model " + model
                        + " timeout — thử thêm một model dự phòng...");
            }
        }
        return null;
    }

    private GeminiResponse callGeminiAPI(String modelAndAction,
                                         String requestBody,
                                         int timeoutSeconds) {
        for (int attempt = 1; attempt <= 2; attempt++) {
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
                System.err.println("[GeminiService] API trả về HTTP "
                        + status
                        + " (" + modelAndAction + ") | "
                        + body.substring(0, Math.min(200, body.length())));

                if (status == 429) {
                    return GeminiResponse.quota();
                }
                boolean retryable = status == 408 || status == 500
                        || status == 502 || status == 503 || status == 504;
                if (!retryable) {
                    return GeminiResponse.fail();
                }
                if (attempt < 2) {
                    Thread.sleep(600L);
                }
            } catch (HttpTimeoutException timeout) {
                // Không retry ngay cùng một request: timeout đã chờ đủ lâu
                // và retry mù sẽ làm người dùng phải chờ thêm nhiều phút.
                System.err.println("[GeminiService] Lỗi gọi API ("
                        + modelAndAction + ", timeout=" + timeoutSeconds
                        + "s): request timed out.");
                return GeminiResponse.timeout();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return GeminiResponse.fail();
            } catch (IOException ioe) {
                System.err.println("[GeminiService] Lỗi mạng gọi API ("
                        + modelAndAction + "): " + ioe.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(600L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return GeminiResponse.fail();
                    }
                }
            } catch (Exception e) {
                System.err.println("[GeminiService] Lỗi gọi API ("
                        + modelAndAction + "): " + e.getMessage());
                if (attempt < 2) {
                    try {
                        Thread.sleep(600L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return GeminiResponse.fail();
                    }
                }
            }
        }
        return GeminiResponse.fail();
    }

    private String tryImagen3Predict(String prompt) {
        String[] imagenModels = {"imagen-3.0-generate-002", "imagen-3.0-fast-generate-001"};

        JsonObject root = new JsonObject();
        JsonArray instances = new JsonArray();
        JsonObject inst = new JsonObject();
        inst.addProperty("prompt", prompt);
        instances.add(inst);
        root.add("instances", instances);

        JsonObject params = new JsonObject();
        params.addProperty("sampleCount", 1);
        params.addProperty("aspectRatio", "16:9");
        params.addProperty("outputMimeType", "image/jpeg");
        root.add("parameters", params);

        String requestBody = GSON.toJson(root);

        for (String model : imagenModels) {
            try {
                String url = BASE_URL + model + ":predict?key=" + API_KEY;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(IMAGE_TIMEOUT_SECONDS))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    JsonObject respJson = JsonParser.parseString(response.body()).getAsJsonObject();
                    if (respJson.has("predictions")) {
                        JsonArray preds = respJson.getAsJsonArray("predictions");
                        if (!preds.isEmpty()) {
                            JsonObject pred = preds.get(0).getAsJsonObject();
                            if (pred.has("bytesBase64Encoded")) {
                                String base64 = pred.get("bytesBase64Encoded").getAsString();
                                String mime = pred.has("mimeType") ? pred.get("mimeType").getAsString() : "image/jpeg";
                                String saved = saveBase64Image(base64, mime);
                                if (saved != null) {
                                    System.out.println("[GeminiService] Tạo ảnh thành công bằng Imagen: " + model);
                                    return saved;
                                }
                            }
                        }
                    }
                } else {
                    System.err.println("[GeminiService] Imagen " + model + " trả về: " + response.statusCode());
                }
            } catch (Exception e) {
                System.err.println("[GeminiService] Lỗi Imagen " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String tryGeminiNativeImage(String prompt) {
        String[] imageModels = {"gemini-2.0-flash-exp", "gemini-2.0-flash"};
        String requestBody = buildNativeImageRequestBody(prompt);

        for (String model : imageModels) {
            try {
                String url = BASE_URL + model + ":generateContent?key=" + API_KEY;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .timeout(Duration.ofSeconds(IMAGE_TIMEOUT_SECONDS))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    InlineImage image = extractInlineImage(response.body());
                    if (image != null) {
                        String saved = saveBase64Image(image.data(), image.mimeType());
                        if (saved != null) {
                            System.out.println("[GeminiService] Tạo ảnh thành công bằng Gemini native: " + model);
                            return saved;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[GeminiService] Lỗi Gemini native " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Fallback tạo ảnh AI miễn phí bằng Pollinations AI (không giới hạn quota/rate limit).
     */
    private String tryFreeAiImage(String prompt) {
        try {
            System.out.println("[GeminiService] Đang tạo ảnh banner sự kiện bằng Free AI Image Generator...");
            String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
            int seed = new Random().nextInt(1_000_000);
            String url = "https://image.pollinations.ai/prompt/" + encoded
                    + "?width=1280&height=720&nologo=true&seed=" + seed;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) EventHubAI/1.0")
                    .GET()
                    .timeout(Duration.ofSeconds(IMAGE_TIMEOUT_SECONDS))
                    .build();

            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 1000) {
                String fileName = "ai_" + UUID.randomUUID() + ".jpg";
                Path uploadPath = UploadConfig.getBaseDir().resolve("events");
                Files.createDirectories(uploadPath);
                Files.write(uploadPath.resolve(fileName), response.body());
                System.out.println("[GeminiService] Tạo ảnh AI thành công (Free AI): " + fileName);
                return fileName;
            } else {
                System.err.println("[GeminiService] Free AI Image trả về HTTP: " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi Free AI Image: " + e.getMessage());
        }
        return null;
    }

    private record GeminiResponse(String body,
                                  boolean quotaExceeded,
                                  boolean timedOut) {
        static GeminiResponse ok(String body) {
            return new GeminiResponse(body, false, false);
        }

        static GeminiResponse quota() {
            return new GeminiResponse(null, true, false);
        }

        static GeminiResponse timeout() {
            return new GeminiResponse(null, false, true);
        }

        static GeminiResponse fail() {
            return new GeminiResponse(null, false, false);
        }

        boolean ok() {
            return body != null && !quotaExceeded && !timedOut;
        }
    }

    private String extractTextFromResponse(String responseJson) {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();

            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            JsonObject candidate = candidates.get(0).getAsJsonObject();
            if (candidate.has("finishReason")
                    && "MAX_TOKENS".equals(candidate.get("finishReason").getAsString())) {
                System.err.println("[GeminiService] Câu trả lời chạm giới hạn maxOutputTokens.");
            }
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
            System.err.println("[GeminiService] Lỗi parse text response: " + e.getMessage());
            return null;
        }
    }

    private record InlineImage(String data, String mimeType) {
    }

    private InlineImage extractInlineImage(String responseJson) {
        try {
            JsonObject json = JsonParser.parseString(responseJson).getAsJsonObject();
            JsonArray candidates = json.getAsJsonArray("candidates");
            if (candidates == null || candidates.isEmpty()) {
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
            return null;
        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi parse inlineData: " + e.getMessage());
            return null;
        }
    }

    private String saveBase64Image(String base64Data, String mimeType) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String extension = extensionFromMime(mimeType);
            String fileName = "ai_" + UUID.randomUUID() + "." + extension;

            Path uploadPath = UploadConfig.getBaseDir().resolve("events");
            Files.createDirectories(uploadPath);
            Files.write(uploadPath.resolve(fileName), imageBytes);

            System.out.println("[GeminiService] Đã lưu ảnh AI: " + fileName);
            return fileName;
        } catch (Exception e) {
            System.err.println("[GeminiService] Lỗi lưu ảnh Base64: " + e.getMessage());
            return null;
        }
    }

    private static String extensionFromMime(String mimeType) {
        if (mimeType == null) return "jpg";
        String mime = mimeType.toLowerCase();
        if (mime.contains("jpeg") || mime.contains("jpg")) return "jpg";
        if (mime.contains("webp")) return "webp";
        if (mime.contains("png")) return "png";
        return "jpg";
    }
}