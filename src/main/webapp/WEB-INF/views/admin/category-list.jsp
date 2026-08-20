<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle"   value="Quản lý danh mục" scope="request"/>
<c:set var="topbarTitle" value="Quản lý danh mục" scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">

        <%-- PAGE HEADER --%>
        <div class="page-header">
            <div>
                <h1><i class="bi bi-tags"></i> Quản lý danh mục</h1>
                <div class="breadcrumb text-muted">Phân loại các sự kiện trong hệ thống</div>
            </div>
            <button class="btn-admin-primary" data-bs-toggle="modal" data-bs-target="#addCategoryModal">
                <i class="bi bi-plus-lg"></i> Thêm danh mục mới
            </button>
        </div>

        <%-- CẢNH BÁO LỖI NẾU CÓ --%>
        <c:if test="${not empty errorMsg}">
            <div class="alert alert-danger alert-dismissible fade show">
                <i class="bi bi-exclamation-triangle"></i> ${errorMsg}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </c:if>

        <%-- BẢNG DANH MỤC --%>
        <div class="admin-card p-0">
            <div class="table-responsive">
                <table class="admin-table mb-0">
                    <thead>
                    <tr>
                        <th style="width: 60px;">ID</th>
                        <th>Tên danh mục</th>
                        <th>Mô tả</th>
                        <th style="width: 130px;">Trạng thái</th>
                        <th style="width: 140px;">Hành động</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:forEach var="cat" items="${categories}">
                        <tr>
                            <td><strong>#${cat.categoryId}</strong></td>
                            <td class="fw-semibold">${cat.categoryName}</td>
                            <td class="text-muted">${empty cat.description ? '—' : cat.description}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${cat.active}">
                                        <span class="status-badge published">Hoạt động</span>
                                    </c:when>
                                    <c:otherwise>
                                        <span class="status-badge cancelled">Khóa</span>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>
                                    <%-- Nút Sửa (Mở Modal) --%>
                                <button class="btn-icon" title="Sửa"
                                        onclick="openEditModal(${cat.categoryId}, '${cat.categoryName}', '${cat.description}')">
                                    <i class="bi bi-pencil"></i>
                                </button>

                                    <%-- Nút Toggle Trạng thái --%>
                                <c:choose>
                                    <c:when test="${cat.active}">
                                        <form method="post" action="${pageContext.request.contextPath}/admin/categories"
                                              class="d-inline confirm-form"
                                              data-confirm="Bạn muốn vô hiệu hóa danh mục '${cat.categoryName}'?">
                                            <input type="hidden" name="action" value="deactivate">
                                            <input type="hidden" name="categoryId" value="${cat.categoryId}">
                                            <button type="submit" class="btn-icon btn-icon-danger" title="Vô hiệu hóa">
                                                <i class="bi bi-slash-circle"></i>
                                            </button>
                                        </form>
                                    </c:when>
                                    <c:otherwise>
                                        <form method="post" action="${pageContext.request.contextPath}/admin/categories"
                                              class="d-inline">
                                            <input type="hidden" name="action" value="activate">
                                            <input type="hidden" name="categoryId" value="${cat.categoryId}">
                                            <button type="submit" class="btn-icon text-success" title="Kích hoạt lại">
                                                <i class="bi bi-check-circle"></i>
                                            </button>
                                        </form>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </div>
        </div>

    </div>
</div>

<%-- MODAL THÊM DANH MỤC --%>
<div class="modal fade" id="addCategoryModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/admin/categories">
                <input type="hidden" name="action" value="add">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-folder-plus"></i> Thêm danh mục mới</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Tên danh mục <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" name="categoryName" required maxlength="100">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Mô tả</label>
                        <textarea class="form-control" name="description" rows="3" maxlength="500"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn-admin-primary">Lưu danh mục</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- MODAL SỬA DANH MỤC --%>
<div class="modal fade" id="editCategoryModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/admin/categories">
                <input type="hidden" name="action" value="edit">
                <input type="hidden" name="categoryId" id="editCatId">
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-pencil-square"></i> Sửa danh mục</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label">Tên danh mục <span class="text-danger">*</span></label>
                        <input type="text" class="form-control" name="categoryName" id="editCatName" required maxlength="100">
                    </div>
                    <div class="mb-3">
                        <label class="form-label">Mô tả</label>
                        <textarea class="form-control" name="description" id="editCatDesc" rows="3" maxlength="500"></textarea>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Hủy</button>
                    <button type="submit" class="btn-admin-primary">Cập nhật</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function openEditModal(id, name, desc) {
        document.getElementById('editCatId').value = id;
        document.getElementById('editCatName').value = name;
        document.getElementById('editCatDesc').value = desc === 'null' ? '' : desc;
        new bootstrap.Modal(document.getElementById('editCategoryModal')).show();
    }
</script>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>