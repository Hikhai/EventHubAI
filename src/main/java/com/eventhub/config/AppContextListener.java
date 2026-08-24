package com.eventhub.config;

import com.eventhub.service.ChatbotService;
import com.eventhub.service.PaymentExpiryService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Khởi tạo thư mục ảnh trong project và connection pool khi app start.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            UploadConfig.init(sce.getServletContext());
        } catch (Exception e) {
            System.err.println("[AppContextListener] Chưa khởi tạo thư mục ảnh: " + e.getMessage());
        }
        try {
            DBConnection.init();
            PaymentExpiryService.start();
        } catch (Exception e) {
            System.err.println("[AppContextListener] Chưa khởi tạo DB pool: " + e.getMessage());
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ChatbotService.shutdownExecutor();
        PaymentExpiryService.shutdown();
        DBConnection.shutdown();
    }
}
