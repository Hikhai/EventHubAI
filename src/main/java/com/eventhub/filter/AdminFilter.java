package com.eventhub.filter;

import com.eventhub.model.User;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;

import java.io.IOException;

/**
 * Filter kiểm tra quyền ADMIN.
 * Chạy SAU AuthFilter (AuthFilter đã đảm bảo user đã đăng nhập).
 * Nếu user không phải ADMIN → trả về lỗi 403 Forbidden.
 */
@WebFilter(urlPatterns = {"/admin/*"})
public class AdminFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest  req  = (HttpServletRequest)  request;
        HttpServletResponse resp = (HttpServletResponse) response;

        // Lấy user từ session (AuthFilter đã đảm bảo không null)
        User user = (User) req.getSession().getAttribute("loggedInUser");

        if (user == null || !user.isAdmin()) {
            // Không phải ADMIN → 403
            resp.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập trang này.");
            return;
        }

        // Là ADMIN → cho qua
        chain.doFilter(request, response);
    }
}