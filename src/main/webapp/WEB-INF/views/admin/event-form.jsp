<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="pageTitle"
       value="${isEditing ? 'Sửa sự kiện' : 'Tạo sự kiện mới'}"
       scope="request"/>
<c:set var="topbarTitle"
       value="${isEditing ? 'Chỉnh sửa sự kiện' : 'Tạo sự kiện mới'}"
       scope="request"/>

<jsp:include page="/WEB-INF/views/common/header-admin.jsp"/>
<jsp:include page="/WEB-INF/views/common/navbar-admin.jsp"/>

<div class="admin-main">
    <jsp:include page="/WEB-INF/views/common/topbar-admin.jsp"/>

    <div class="admin-content">

        <%-- ===== PAGE HEADER ===== --%>
        <div class="page-header">
            <div>
                <h1>
                    <i class="bi ${isEditing ? 'bi-pencil-square' : 'bi-plus-circle'}"></i>
                    ${isEditing ? 'Chỉnh sửa sự kiện' : 'Tạo sự kiện mới'}
                </h1>
                <div class="breadcrumb text-muted">
                    <a href="${pageContext.request.contextPath}/admin/events"
                       class="text-decoration-none">
                        Sự kiện
                    </a>
                    /
                    ${isEditing ? 'Chỉnh sửa' : 'Tạo mới'}
                </div>
            </div>
            <a href="${pageContext.request.contextPath}/admin/events"
               class="btn-admin-secondary">
                <i class="bi bi-arrow-left"></i> Quay lại
            </a>
        </div>

        <%-- Hiển thị lỗi validation --%>
        <c:if test="${not empty errorMsg}">
            <div class="alert alert-danger">
                <i class="bi bi-exclamation-triangle"></i> ${errorMsg}
            </div>
        </c:if>

        <%-- ===== FORM ===== --%>
        <form method="post"
              action="${pageContext.request.contextPath}${isEditing ? '/admin/events/edit' : '/admin/events/create'}"
              enctype="multipart/form-data"
              class="admin-form"
              id="eventForm">

            <c:if test="${isEditing}">
                <input type="hidden" name="eventId" value="${event.eventId}">
            </c:if>

            <div class="row">
                <%-- ===== LEFT COLUMN: Thông tin chính ===== --%>
                <div class="col-lg-8">
                    <div class="form-section">
                        <div class="form-section-title">
                            <i class="bi bi-info-circle"></i>
                            Thông tin cơ bản
                        </div>

                        <%-- Tiêu đề --%>
                        <div class="mb-3">
                            <label class="form-label required">
                                Tên sự kiện <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control"
                                   name="title" id="title"
                                   value="${event.title}"
                                   minlength="5" maxlength="200"
                                   required>
                            <div class="form-hint">5-200 ký tự</div>
                        </div>

                        <%-- Mô tả + AI Summary --%>
                        <div class="mb-3">
                            <label class="form-label">
                                Mô tả chi tiết <span class="text-danger">*</span>
                            </label>
                            <textarea class="form-control"
                                      name="description" id="description"
                                      rows="6" minlength="20"
                                      required>${event.description}</textarea>
                            <div class="d-flex justify-content-between align-items-center mt-1">
                                <div class="form-hint">Tối thiểu 20 ký tự</div>
                                <div class="char-counter">
                                    <span id="descCounter">0</span> ký tự
                                </div>
                            </div>
                        </div>

                        <%-- AI Summary --%>
                        <div class="mb-3">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <label class="form-label mb-0">
                                    Tóm tắt AI
                                    <small class="text-muted">(hiển thị ở card sự kiện)</small>
                                </label>
                                <button type="button" class="btn-ai-helper" id="btnGenSummary">
                                    <i class="bi bi-magic"></i>
                                    <span>Tạo tóm tắt bằng Gemini AI</span>
                                </button>
                            </div>
                            <textarea class="form-control"
                                      name="summaryAi" id="summaryAi"
                                      rows="3"
                                      placeholder="Điền thủ công hoặc nhấn nút để AI tự tạo..."
                            >${event.summaryAi}</textarea>
                            <div class="form-hint">
                                Tối đa 200 ký tự. Nếu để trống, hệ thống sẽ dùng 150 ký tự đầu của mô tả.
                            </div>
                        </div>

                        <%-- Địa điểm --%>
                        <div class="mb-3">
                            <label class="form-label">
                                Địa điểm <span class="text-danger">*</span>
                            </label>
                            <input type="text" class="form-control"
                                   name="location"
                                   value="${event.location}"
                                   maxlength="300"
                                   required>
                        </div>
                    </div>

                    <%-- ===== Thời gian ===== --%>
                    <div class="form-section">
                        <div class="form-section-title">
                            <i class="bi bi-clock"></i>
                            Thời gian
                        </div>

                        <div class="row">
                            <div class="col-md-4 mb-3">
                                <label class="form-label">
                                    Bắt đầu <span class="text-danger">*</span>
                                </label>
                                <input type="datetime-local" class="form-control"
                                       name="startTime"
                                       value="${event.startTimeInput}"
                                       required>
                            </div>

                            <div class="col-md-4 mb-3">
                                <label class="form-label">
                                    Kết thúc <span class="text-danger">*</span>
                                </label>
                                <input type="datetime-local" class="form-control"
                                       name="endTime"
                                       value="${event.endTimeInput}"
                                       required>
                            </div>

                            <div class="col-md-4 mb-3">
                                <label class="form-label">
                                    Hạn đăng ký <span class="text-danger">*</span>
                                </label>
                                <input type="datetime-local" class="form-control"
                                       name="registrationDeadline"
                                       value="${event.registrationDeadlineInput}"
                                       required>
                            </div>
                        </div>

                        <div class="form-hint">
                            <i class="bi bi-info-circle"></i>
                            Hạn đăng ký phải trước hoặc bằng thời gian bắt đầu.
                            Thời gian kết thúc phải sau thời gian bắt đầu.
                        </div>
                    </div>
                </div>

                <%-- ===== RIGHT COLUMN: Cấu hình + Ảnh ===== --%>
                <div class="col-lg-4">
                    <%-- Cấu hình --%>
                    <div class="form-section">
                        <div class="form-section-title">
                            <i class="bi bi-gear"></i>
                            Cấu hình
                        </div>

                        <%-- Danh mục --%>
                        <div class="mb-3">
                            <label class="form-label">
                                Danh mục <span class="text-danger">*</span>
                            </label>
                            <select class="form-select" name="categoryId" required>
                                <option value="">-- Chọn danh mục --</option>
                                <c:forEach var="cat" items="${categories}">
                                    <option value="${cat.categoryId}"
                                        ${event.categoryId == cat.categoryId ? 'selected' : ''}>
                                            ${cat.categoryName}
                                    </option>
                                </c:forEach>
                            </select>
                        </div>

                        <%-- Số lượng tối đa --%>
                        <div class="mb-3">
                            <label class="form-label">
                                Số người tối đa <span class="text-danger">*</span>
                            </label>
                            <input type="number" class="form-control"
                                   name="maxParticipants"
                                   value="${event.maxParticipants > 0 ? event.maxParticipants : 30}"
                                   min="1" max="10000"
                                   required>
                        </div>

                        <%-- Trạng thái --%>
                        <div class="mb-3">
                            <label class="form-label">
                                Trạng thái <span class="text-danger">*</span>
                            </label>
                            <select class="form-select" name="status" required>
                                <option value="DRAFT"
                                ${event.status == 'DRAFT' ? 'selected' : ''}>
                                    📝 Nháp (chỉ admin thấy)
                                </option>
                                <option value="PUBLISHED"
                                ${event.status == 'PUBLISHED' ? 'selected' : ''}>
                                    🚀 Đang mở (User thấy được)
                                </option>
                            </select>
                        </div>
                    </div>

                    <%-- Ảnh sự kiện --%>
                    <div class="form-section">
                        <div class="form-section-title">
                            <i class="bi bi-image"></i>
                            Ảnh sự kiện
                        </div>

                        <c:if test="${isEditing && not empty event.imagePath}">
                            <div class="mb-3">
                                <label class="form-label">Ảnh hiện tại:</label>
                                <img src="${pageContext.request.contextPath}${event.displayImagePath}"
                                     class="img-fluid rounded"
                                     style="max-height: 180px;"
                                     onerror="this.src='${pageContext.request.contextPath}/uploads/defaults/default_other.jpg'">
                                <div class="form-hint mt-1">
                                    <c:choose>
                                        <c:when test="${event.imageSource == 'UPLOADED'}">
                                            <i class="bi bi-upload"></i> Đã upload
                                        </c:when>
                                        <c:when test="${event.imageSource == 'AI_GENERATED'}">
                                            <i class="bi bi-robot"></i> Do AI tạo
                                        </c:when>
                                        <c:otherwise>
                                            <i class="bi bi-image"></i> Ảnh mặc định
                                        </c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </c:if>

                        <label class="image-preview-box" for="imageFile" id="imagePreviewBox">
                            <i class="bi bi-cloud-upload"></i>
                            <div class="fw-semibold">Nhấn để tải ảnh lên</div>
                            <div class="image-preview-info">
                                JPG, PNG, WEBP (tối đa 5MB)
                            </div>
                        </label>

                        <input type="file" name="imageFile" id="imageFile"
                               accept="image/jpeg,image/png,image/webp"
                               class="d-none">

                        <c:choose>
                            <c:when test="${isEditing}">
                                <label class="ai-image-option mt-3" for="regenerateAi">
                                    <input type="checkbox" name="regenerateAi" id="regenerateAi" value="1">
                                    <span class="ai-image-option-body">
                                        <span class="ai-image-option-title">
                                            <i class="bi bi-magic"></i>
                                            Tạo ảnh mới bằng AI (thay ảnh hiện tại)
                                        </span>
                                        <span class="ai-image-option-desc">
                                            Bỏ trống file upload và tick ô này để AI tạo poster mới.
                                            Nếu AI lỗi, ảnh cũ được giữ lại.
                                        </span>
                                    </span>
                                </label>
                            </c:when>
                            <c:otherwise>
                                <div class="form-hint mt-2">
                                    <i class="bi bi-magic text-primary"></i>
                                    <strong>Bỏ trống</strong> để AI tự tạo ảnh poster cho sự kiện.
                                </div>
                            </c:otherwise>
                        </c:choose>
                    </div>
                </div>
            </div>

            <%-- ===== ACTIONS BAR ===== --%>
            <div class="form-actions-bar">
                <a href="${pageContext.request.contextPath}/admin/events"
                   class="btn-admin-secondary">
                    Hủy
                </a>
                <button type="submit" class="btn-admin-primary" id="submitBtn">
                    <i class="bi bi-check-circle"></i>
                    ${isEditing ? 'Cập nhật sự kiện' : 'Tạo sự kiện'}
                </button>
            </div>
        </form>
    </div>
</div>

<%-- ===== SCRIPTS ===== --%>
<script>
    document.addEventListener('DOMContentLoaded', function() {

        // ===== CHARACTER COUNTER =====
        const descriptionField = document.getElementById('description');
        const descCounter = document.getElementById('descCounter');

        function updateCounter() {
            descCounter.textContent = descriptionField.value.length;
        }
        descriptionField.addEventListener('input', updateCounter);
        updateCounter(); // Init lần đầu

        // ===== IMAGE PREVIEW =====
        const imageInput = document.getElementById('imageFile');
        const imageBox   = document.getElementById('imagePreviewBox');
        const regenerateAi = document.getElementById('regenerateAi');

        imageInput.addEventListener('change', function(e) {
            const file = e.target.files[0];
            if (!file) return;

            // Validate size (5MB)
            if (file.size > 5 * 1024 * 1024) {
                alert('File quá lớn! Tối đa 5MB.');
                imageInput.value = '';
                return;
            }

            if (regenerateAi) regenerateAi.checked = false;

            // Show preview
            const reader = new FileReader();
            reader.onload = function(ev) {
                imageBox.innerHTML =
                    '<img src="' + ev.target.result + '">' +
                    '<div class="fw-semibold">' + file.name + '</div>' +
                    '<div class="image-preview-info">' +
                    (file.size / 1024).toFixed(1) + ' KB — Nhấn để đổi ảnh khác' +
                    '</div>';
            };
            reader.readAsDataURL(file);
        });

        if (regenerateAi) {
            regenerateAi.addEventListener('change', function() {
                if (!this.checked) return;
                imageInput.value = '';
                imageBox.innerHTML =
                    '<i class="bi bi-cloud-upload"></i>' +
                    '<div class="fw-semibold">Nhấn để tải ảnh lên</div>' +
                    '<div class="image-preview-info">JPG, PNG, WEBP (tối đa 5MB)</div>';
            });
        }

        // ===== AI SUMMARY BUTTON =====
        const btnGenSummary = document.getElementById('btnGenSummary');
        const titleField    = document.getElementById('title');
        const summaryField  = document.getElementById('summaryAi');

        btnGenSummary.addEventListener('click', function() {
            const title = titleField.value.trim();
            const desc  = descriptionField.value.trim();

            if (!title || !desc) {
                alert('Vui lòng nhập Tên sự kiện và Mô tả trước khi tạo tóm tắt!');
                return;
            }

            if (desc.length < 20) {
                alert('Mô tả cần ít nhất 20 ký tự để tạo tóm tắt.');
                return;
            }

            // Disable button + show loading
            btnGenSummary.disabled = true;
            const originalHtml = btnGenSummary.innerHTML;
            btnGenSummary.innerHTML =
                '<span class="spinner-border spinner-border-sm"></span> Đang tạo...';

            // Call API
            const formData = new URLSearchParams();
            formData.append('title', title);
            formData.append('description', desc);

            const contextPath = document.querySelector('meta[name="context-path"]')
                .getAttribute('content');

            fetch(contextPath + '/api/ai/summary', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'
                },
                body: formData.toString()
            })
                .then(res => res.json())
                .then(data => {
                    if (data.success && data.summary) {
                        summaryField.value = data.summary;
                        summaryField.style.background = '#DCFCE7';
                        setTimeout(() => summaryField.style.background = '', 1500);
                    } else {
                        alert('❌ ' + (data.message || 'Không thể tạo tóm tắt.'));
                    }
                })
                .catch(err => {
                    alert('❌ Lỗi kết nối: ' + err.message);
                })
                .finally(() => {
                    btnGenSummary.disabled = false;
                    btnGenSummary.innerHTML = originalHtml;
                });
        });

        // ===== FORM VALIDATION TRƯỚC KHI SUBMIT =====
        document.getElementById('eventForm').addEventListener('submit', function(e) {
            const start = document.querySelector('[name="startTime"]').value;
            const end   = document.querySelector('[name="endTime"]').value;
            const dl    = document.querySelector('[name="registrationDeadline"]').value;

            if (start && end && new Date(end) <= new Date(start)) {
                e.preventDefault();
                alert('Thời gian kết thúc phải sau thời gian bắt đầu!');
                return;
            }

            if (start && dl && new Date(dl) > new Date(start)) {
                e.preventDefault();
                alert('Hạn đăng ký phải trước hoặc bằng thời gian bắt đầu!');
                return;
            }

            // Disable submit button để tránh double-submit
            const btn = document.getElementById('submitBtn');
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Đang xử lý...';
        });
    });
</script>

<jsp:include page="/WEB-INF/views/common/footer-admin.jsp"/>