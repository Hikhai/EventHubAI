package com.eventhub.servlet.auth;

import com.eventhub.exception.AuthException;
import com.eventhub.model.User;
import com.eventhub.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý đăng nhập.
 * GET  /auth/login → Hiển thị form
 * POST /auth/login → Xử lý đăng nhập
 */
@WebServlet("/auth/login")
public class LoginServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    /** GET: Hiển thị trang đăng nhập */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Nếu đã đăng nhập → redirect về trang phù hợp
        User user = (User) req.getSession().getAttribute("loggedInUser");
        if (user != null) {
            redirectByRole(user, req, resp);
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                .forward(req, resp);
    }

    /** POST: Xử lý đăng nhập */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Đặt encoding trước khi đọc param
        req.setCharacterEncoding("UTF-8");

        String email    = req.getParameter("email");
        String password = req.getParameter("password");

        try {
            // Gọi service xử lý
            User user = authService.login(email, password);

            // Tạo session MỚI sau đăng nhập (tránh session fixation attack)
            req.getSession().invalidate();
            HttpSession newSession = req.getSession(true);
            newSession.setAttribute("loggedInUser", user);

            // Lấy URL để redirect (nếu user bị chặn trước đó)
            String redirectUrl = (String) newSession.getAttribute("redirectAfterLogin");
            newSession.removeAttribute("redirectAfterLogin");

            if (redirectUrl != null && !redirectUrl.isBlank()) {
                resp.sendRedirect(redirectUrl);
            } else {
                redirectByRole(user, req, resp);
            }

        } catch (AuthException e) {
            // Đăng nhập thất bại → hiển thị lỗi, giữ lại email
            req.setAttribute("errorMsg", e.getMessage());
            req.setAttribute("email", email);  // Giữ email để user không phải nhập lại
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            // Lỗi hệ thống
            req.setAttribute("errorMsg", "Lỗi hệ thống, vui lòng thử lại.");
            req.getRequestDispatcher("/WEB-INF/views/auth/login.jsp")
                    .forward(req, resp);
        }
    }

    /** Redirect theo role sau khi đăng nhập thành công */
    private void redirectByRole(User user, HttpServletRequest req,
                                HttpServletResponse resp) throws IOException {
        if (user.isAdmin()) {
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        } else {
            resp.sendRedirect(req.getContextPath() + "/events");
        }
    }
}