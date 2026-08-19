package com.eventhub.servlet.auth;

import com.eventhub.exception.AuthException;
import com.eventhub.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Xử lý đăng ký tài khoản.
 * GET  /auth/register → Hiển thị form
 * POST /auth/register → Xử lý đăng ký
 */
@WebServlet("/auth/register")
public class RegisterServlet extends HttpServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        // Đã đăng nhập → không cho vào trang đăng ký
        if (req.getSession().getAttribute("loggedInUser") != null) {
            resp.sendRedirect(req.getContextPath() + "/events");
            return;
        }

        req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp")
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");

        String fullName        = req.getParameter("fullName");
        String email           = req.getParameter("email");
        String password        = req.getParameter("password");
        String confirmPassword = req.getParameter("confirmPassword");

        try {
            authService.register(fullName, email, password, confirmPassword);

            // Đăng ký thành công → flash message + redirect login
            req.getSession().setAttribute("successMsg",
                    "Đăng ký thành công! Vui lòng đăng nhập.");
            resp.sendRedirect(req.getContextPath() + "/auth/login");

        } catch (AuthException e) {
            // Validation thất bại → giữ lại fullName và email (không giữ password)
            req.setAttribute("errorMsg",  e.getMessage());
            req.setAttribute("fullName",  fullName);
            req.setAttribute("email",     email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.setAttribute("errorMsg", "Lỗi hệ thống, vui lòng thử lại.");
            req.setAttribute("fullName", fullName);
            req.setAttribute("email",    email);
            req.getRequestDispatcher("/WEB-INF/views/auth/register.jsp")
                    .forward(req, resp);
        }
    }
}