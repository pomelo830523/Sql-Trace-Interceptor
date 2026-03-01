package com.example.g85report.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SqlTraceInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("true".equalsIgnoreCase(request.getHeader("X-Trace-SQL"))) {
            SqlCaptureHolder.startCapture();
            QueryResultCaptureHolder.startCapture();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        SqlCaptureHolder.stopCapture();
        QueryResultCaptureHolder.stopCapture();
    }
}
