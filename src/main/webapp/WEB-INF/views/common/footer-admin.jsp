<%@ page contentType="text/html;charset=UTF-8" %>
<%--
    Admin Footer + Chatbot + Scripts
--%>

<%-- Chatbot vẫn hiển thị cho admin --%>
<jsp:include page="/WEB-INF/views/common/chatbot.jsp"/>

<%-- Scripts --%>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/main.js"></script>
<script src="${pageContext.request.contextPath}/assets/js/chatbot.js"></script>

<%-- Toggle sidebar mobile --%>
<script>
    document.addEventListener('DOMContentLoaded', function() {
        const toggleBtn = document.getElementById('sidebarToggle');
        const sidebar = document.getElementById('adminSidebar');

        if (toggleBtn && sidebar) {
            toggleBtn.addEventListener('click', function() {
                sidebar.classList.toggle('show');
            });

            // Đóng sidebar khi click ngoài (mobile)
            document.addEventListener('click', function(e) {
                if (window.innerWidth < 992
                    && !sidebar.contains(e.target)
                    && !toggleBtn.contains(e.target)) {
                    sidebar.classList.remove('show');
                }
            });
        }
    });
</script>

</body>
</html>