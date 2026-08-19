package com.eventhub.servlet.admin;

import com.eventhub.dao.CategoryDAO;
import com.eventhub.model.Category;
import com.eventhub.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.List;

/**
 * Quản lý danh mục sự kiện.
 * GET  /admin/categories            → Danh sách
 * POST /admin/categories?action=add → Thêm mới
 * POST /admin/categories?action=edit     → Sửa
 * POST /admin/categories?action=deactivate → Vô hiệu hóa
 * POST /admin/categories?action=activate   → Kích hoạt lại
 */
@WebServlet("/admin/categories")
public class AdminCategoryServlet extends HttpServlet {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            // Admin xem tất cả (kể cả inactive)
            List<Category> categories = categoryDAO.findAllForAdmin();
            req.setAttribute("categories", categories);

            req.getRequestDispatcher("/WEB-INF/views/admin/category-list.jsp")
                    .forward(req, resp);

        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", "Lỗi tải danh mục.");
            resp.sendRedirect(req.getContextPath() + "/admin/dashboard");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        String action = req.getParameter("action");

        try {
            switch (action != null ? action : "") {

                case "add" -> handleAdd(req);
                case "edit" -> handleEdit(req);
                case "deactivate" -> handleDeactivate(req);
                case "activate" -> handleActivate(req);
                default -> req.getSession().setAttribute("errorMsg",
                        "Hành động không hợp lệ.");
            }
        } catch (Exception e) {
            req.getSession().setAttribute("errorMsg", e.getMessage());
        }

        resp.sendRedirect(req.getContextPath() + "/admin/categories");
    }

    private void handleAdd(HttpServletRequest req) throws Exception {
        String name = req.getParameter("categoryName");
        String desc = req.getParameter("description");

        if (ValidationUtil.isBlank(name)) {
            throw new Exception("Tên danh mục không được để trống.");
        }
        if (categoryDAO.existsByName(name.trim())) {
            throw new Exception("Tên danh mục đã tồn tại: " + name.trim());
        }

        Category cat = new Category();
        cat.setCategoryName(name.trim());
        cat.setDescription(ValidationUtil.isBlank(desc) ? null : desc.trim());
        categoryDAO.insert(cat);

        req.getSession().setAttribute("successMsg", "Thêm danh mục thành công!");
    }

    private void handleEdit(HttpServletRequest req) throws Exception {
        int id = Integer.parseInt(req.getParameter("categoryId"));
        String name = req.getParameter("categoryName");
        String desc = req.getParameter("description");

        if (ValidationUtil.isBlank(name)) {
            throw new Exception("Tên danh mục không được để trống.");
        }
        if (categoryDAO.existsByNameExcluding(name.trim(), id)) {
            throw new Exception("Tên danh mục đã tồn tại.");
        }

        Category cat = new Category();
        cat.setCategoryId(id);
        cat.setCategoryName(name.trim());
        cat.setDescription(ValidationUtil.isBlank(desc) ? null : desc.trim());
        categoryDAO.update(cat);

        req.getSession().setAttribute("successMsg", "Cập nhật danh mục thành công!");
    }

    private void handleDeactivate(HttpServletRequest req) throws Exception {
        int id = Integer.parseInt(req.getParameter("categoryId"));

        // Kiểm tra còn event PUBLISHED đang dùng không
        int publishedCount = categoryDAO.countPublishedEvents(id);
        if (publishedCount > 0) {
            throw new Exception(
                    "Không thể vô hiệu hóa! Có " + publishedCount +
                            " sự kiện đang PUBLISHED dùng danh mục này."
            );
        }

        categoryDAO.deactivate(id);
        req.getSession().setAttribute("successMsg", "Đã vô hiệu hóa danh mục.");
    }

    private void handleActivate(HttpServletRequest req) throws Exception {
        int id = Integer.parseInt(req.getParameter("categoryId"));
        categoryDAO.activate(id);
        req.getSession().setAttribute("successMsg", "Đã kích hoạt lại danh mục.");
    }
}