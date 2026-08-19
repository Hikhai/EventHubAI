<%@ page contentType="text/html;charset=UTF-8" %>
<%--
    Footer + Chatbot widget + JS imports
    Include ở CUỐI mọi trang User
--%>

<footer class="footer">
    <div class="container">
        <div class="row">
            <div class="col-md-4 mb-4">
                <h5><i class="bi bi-calendar-event-fill"></i> EventHub AI</h5>
                <p>Hệ thống quản lý sự kiện dành cho sinh viên và tổ chức,
                    tích hợp AI thông minh giúp tìm kiếm và đăng ký dễ dàng.</p>
            </div>
            <div class="col-md-4 mb-4">
                <h5>Liên kết nhanh</h5>
                <a href="${pageContext.request.contextPath}/events">Danh sách sự kiện</a>
                <a href="${pageContext.request.contextPath}/auth/login">Đăng nhập</a>
                <a href="${pageContext.request.contextPath}/auth/register">Đăng ký</a>
            </div>
            <div class="col-md-4 mb-4">
                <h5>Liên hệ</h5>
                <p><i class="bi bi-envelope"></i> admin@eventhub.com</p>
                <p><i class="bi bi-telephone"></i> 0123 456 789</p>
            </div>
        </div>
        <div class="footer-bottom">
            © 2025 EventHub AI. Đề tài cuối kỳ Java Web.
        </div>
    </div>
</footer>

<%-- ===== CHATBOT WIDGET ===== --%>
<jsp:include page="/WEB-INF/views/common/chatbot.jsp"/>

<%-- ===== JAVASCRIPT ===== --%>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/aos@2.3.4/dist/aos.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/main.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/chatbot.js"></script>

</body>
</html>