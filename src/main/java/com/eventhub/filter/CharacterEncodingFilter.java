package com.eventhub.filter;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebInitParam;

import java.io.IOException;

/**
 * Đặt UTF-8 cho mọi request/response (tiếng Việt trên form POST).
 */
@WebFilter(
        urlPatterns = "/*",
        initParams = @WebInitParam(name = "encoding", value = "UTF-8")
)
public class CharacterEncodingFilter implements Filter {

    private String encoding = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) {
        String configured = filterConfig.getInitParameter("encoding");
        if (configured != null && !configured.isBlank()) {
            encoding = configured;
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding(encoding);
        }
        chain.doFilter(request, response);
    }
}
