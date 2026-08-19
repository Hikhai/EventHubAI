package com.eventhub.filter;

import com.eventhub.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Filter kiểm tra đăng nhập.
 * Áp dụng cho tất cả URL bắt đầu bằng /user/* và /admin/*
 *
 * Luồng:
 *   Có session "loggedInUser" → cho qua
 *   Không có               → lưu URL gốc vào session → redirect /auth/login
 */
@WebFilter(urlPatterns = {"/user/*", "/admin/*"})
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Lấy user từ session
        User user = (User) req.getSession().getAttribute("loggedInUser");

        if (user == null) {
            // Chưa đăng nhập → lưu URL để redirect sau khi login
            String originalUrl = req.getRequestURI();
            String queryString = req.getQueryString();
            if (queryString != null) {
                originalUrl += "?" + queryString;
            }
            req.getSession().setAttribute("redirectAfterLogin", originalUrl);

            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;  // Dừng filter chain
        }

        // Đã đăng nhập → cho request đi tiếp
        chain.doFilter(request, response);
    }
}