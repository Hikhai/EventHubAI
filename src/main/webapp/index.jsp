<%-- src/main/webapp/index.jsp --%>
<%-- Trang tạm để test Tomcat hoạt động --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>EventHub AI — Test</title>
</head>
<body>
<h1>✅ Tomcat đang chạy!</h1>
<p>Context path: <%= request.getContextPath() %></p>
<p>Server: <%= application.getServerInfo() %></p>
<p>Java: <%= System.getProperty("java.version") %></p>
<hr>
<h2>Kiểm tra Database</h2>
<%
    try {
        java.sql.Connection conn =
                com.eventhub.config.DBConnection.getConnection();
        out.println("<p style='color:green'>✅ Database kết nối OK!</p>");
        out.println("<p>Database: " + conn.getCatalog() + "</p>");
        conn.close();
    } catch (Exception e) {
        out.println("<p style='color:red'>❌ Database lỗi: "
                + e.getMessage() + "</p>");
    }
%>
</body>
</html>