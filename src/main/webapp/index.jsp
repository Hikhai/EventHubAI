<%@ page contentType="text/html;charset=UTF-8" %>
<%
    /**
     * Trang gốc: redirect theo trạng thái đăng nhập
     */
    com.eventhub.model.User user =
            (com.eventhub.model.User) session.getAttribute("loggedInUser");

    if (user != null && user.isAdmin()) {
        response.sendRedirect(request.getContextPath() + "/admin/dashboard");
    } else {
        response.sendRedirect(request.getContextPath() + "/events");
    }
%>