<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="pageTitle"    value="Dashboard" scope="request"/>
<c:set var="topbarTitle"  value="Dashboard" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">
        <div class="admin-card">
            <h2>🎯 Test Admin Layout</h2>
            <p>Nếu bạn thấy sidebar bên trái + topbar phía trên → Layout OK!</p>
            <button class="btn-admin-primary">Test button</button>
            <button class="btn-admin-secondary">Cancel</button>
        </div>
    </div>
</div>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>