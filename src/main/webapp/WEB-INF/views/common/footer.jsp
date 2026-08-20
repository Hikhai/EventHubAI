<%@ page contentType="text/html;charset=UTF-8" %>

<footer class="footer">
    <div class="container">
        <div class="row g-4">
            <div class="col-md-5">
                <h5>
                    <span class="brand-mark me-1" style="width:28px;height:28px;font-size:.9rem;display:inline-flex;vertical-align:middle;">
                        <i class="bi bi-calendar-event-fill"></i>
                    </span>
                    EventHub AI
                </h5>
                <p class="mb-0">Nền tảng sự kiện dành cho sinh viên — tìm, đăng ký và gợi ý thông minh với AI.</p>
            </div>
            <div class="col-md-3">
                <h5>Khám phá</h5>
                <a href="${pageContext.request.contextPath}/events">Danh sách sự kiện</a>
                <a href="${pageContext.request.contextPath}/auth/login">Đăng nhập</a>
                <a href="${pageContext.request.contextPath}/auth/register">Tạo tài khoản</a>
            </div>
            <div class="col-md-4">
                <h5>Liên hệ</h5>
                <p class="mb-2"><i class="bi bi-envelope"></i> admin@eventhub.com</p>
                <p class="mb-0"><i class="bi bi-telephone"></i> 0123 456 789</p>
            </div>
        </div>
        <div class="footer-bottom">
            © 2026 EventHub AI · Java Web
        </div>
    </div>
</footer>

<jsp:include page="/WEB-INF/views/common/chatbot.jsp"/>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/aos@2.3.4/dist/aos.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/main.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/chatbot.js"></script>

</body>
</html>
