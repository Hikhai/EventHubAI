package com.eventhub.servlet.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý đăng xuất.
 * POST /auth/logout → Hủy session → Redirect login
 * (Dùng POST để tránh CSRF qua link GET)
 */
@WebServlet("/auth/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Hủy toàn bộ session
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Redirect về trang login
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }

    /** Nếu ai đó GET /auth/logout thì cũng redirect về login */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.sendRedirect(req.getContextPath() + "/auth/login");
    }
}