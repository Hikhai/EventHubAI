package com.eventhub.servlet.admin;

import com.eventhub.dao.CategoryDAO;
import com.eventhub.exception.EventException;
import com.eventhub.model.Category;
import com.eventhub.model.Event;
import com.eventhub.model.User;
import com.eventhub.service.EventService;
import com.eventhub.service.GeminiService;
import com.eventhub.util.DateUtil;
import com.eventhub.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Form tạo và sửa sự kiện cho Admin.
 * GET  /admin/events/create        → Form tạo mới
 * POST /admin/events/create        → Xử lý tạo mới
 * GET  /admin/events/edit?id={id}  → Form sửa
 * POST /admin/events/edit          → Xử lý sửa
 *
 * @MultipartConfig: Bắt buộc để nhận file upload (Part)
 */
@WebServlet(urlPatterns = {"/admin/events/create", "/admin/events/edit"})
@MultipartConfig(
        maxFileSize    = 5 * 1024 * 1024,   // 5MB mỗi file
        maxRequestSize = 6 * 1024 * 1024    // 6MB toàn request
)
public class AdminEventFormServlet extends HttpServlet {

    private final EventService   eventService  = new EventService();
    private final CategoryDAO    categoryDAO   = new CategoryDAO();
    private final GeminiService  geminiService = new GeminiService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        try {
            List<Category> categories = categoryDAO.findAll();
            req.setAttribute("categories", categories);

            String uri = req.getRequestURI();

            if (uri.contains("/edit")) {
                // --- Form SỬA: load dữ liệu cũ vào form ---
                String idStr = req.getParameter("id");
                int eventId = Integer.parseInt(idStr);
                Event event = eventService.getEventById(eventId);
                req.setAttribute("event",     event);
                req.setAttribute("isEditing", true);
            } else {
                // --- Form TẠO MỚI: form rỗng ---
                req.setAttribute("event",     new Event());
                req.setAttribute("isEditing", false);
            }

            req.getRequestDispatcher("/WEB-INF/views/admin/event-form.jsp")
                    .forward(req, resp);

        } catch (EventException e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/events");

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Lỗi tải form sự kiện.");
            resp.sendRedirect(req.getContextPath() + "/admin/events");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        User admin = (User) req.getSession().getAttribute("loggedInUser");

        try {
            // --- Parse tất cả params từ form ---
            Event event = parseEventFromRequest(req);

            // Lấy file upload (có thể null)
            Part imagePart = req.getPart("imageFile");

            String uri = req.getRequestURI();

            if (uri.contains("/edit")) {
                // --- SỬA SỰ KIỆN ---
                String idStr = req.getParameter("eventId");
                event.setEventId(Integer.parseInt(idStr));

                boolean regenerateAi = "1".equals(req.getParameter("regenerateAi"))
                        || "on".equals(req.getParameter("regenerateAi"));

                String imageResult = eventService.updateEvent(event, imagePart, regenerateAi);

                switch (imageResult) {
                    case "AI_OK" -> req.getSession().setAttribute("successMsg",
                            "Cập nhật sự kiện thành công và đã tạo ảnh AI mới.");
                    case "AI_FAILED" -> req.getSession().setAttribute("successMsg",
                            "Cập nhật sự kiện thành công. Không tạo được ảnh AI (quota/lỗi API), ảnh cũ được giữ lại.");
                    case "UPLOAD_FAILED" -> req.getSession().setAttribute("successMsg",
                            "Cập nhật sự kiện thành công nhưng upload ảnh thất bại, ảnh cũ được giữ lại.");
                    default -> req.getSession().setAttribute("successMsg",
                            "Cập nhật sự kiện thành công!");
                }

            } else {
                // --- TẠO MỚI ---
                int newId = eventService.createEvent(event, imagePart,
                        admin.getUserId());

                req.getSession().setAttribute("successMsg",
                        "Tạo sự kiện thành công!");
            }

            resp.sendRedirect(req.getContextPath() + "/admin/events");

        } catch (EventException e) {
            // Validation thất bại → reload form với lỗi
            try {
                req.setAttribute("errorMsg",   e.getMessage());
                req.setAttribute("categories", categoryDAO.findAll());

                // Giữ lại dữ liệu user đã nhập
                Event partialEvent = parseEventFromRequest(req);
                req.setAttribute("event", partialEvent);

                String uri = req.getRequestURI();
                req.setAttribute("isEditing", uri.contains("/edit"));

                req.getRequestDispatcher("/WEB-INF/views/admin/event-form.jsp")
                        .forward(req, resp);
            } catch (Exception ex) {
                resp.sendRedirect(req.getContextPath() + "/admin/events");
            }

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg",
                    "Lỗi hệ thống: " + e.getMessage());
            resp.sendRedirect(req.getContextPath() + "/admin/events");
        }
    }

    /**
     * Parse các trường từ request thành Event object.
     */
    private Event parseEventFromRequest(HttpServletRequest req) {
        Event event = new Event();

        event.setTitle(req.getParameter("title"));
        event.setDescription(req.getParameter("description"));
        event.setSummaryAi(req.getParameter("summaryAi")); // Có thể null
        event.setLocation(req.getParameter("location"));
        event.setStatus(req.getParameter("status"));

        // Parse categoryId
        Integer catId = ValidationUtil.parseIntOrNull(req.getParameter("categoryId"));
        if (catId != null) event.setCategoryId(catId);

        // Parse maxParticipants
        Integer maxP = ValidationUtil.parseIntOrNull(req.getParameter("maxParticipants"));
        if (maxP != null) event.setMaxParticipants(maxP);

        // Parse datetime (HTML datetime-local format: "yyyy-MM-ddTHH:mm")
        event.setStartTime(DateUtil.parseHtmlDateTime(req.getParameter("startTime")));
        event.setEndTime(DateUtil.parseHtmlDateTime(req.getParameter("endTime")));
        event.setRegistrationDeadline(
                DateUtil.parseHtmlDateTime(req.getParameter("registrationDeadline")));

        return event;
    }
}