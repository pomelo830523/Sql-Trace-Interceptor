package com.example.g85report.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AOP aspect that intercepts all repository method calls returning List<?>.
 * Converts each returned entity list to List<Map<String, Object>> and stores
 * it in QueryResultCaptureHolder, in the same order as SQLs are captured by
 * SqlCaptureListener — allowing buildTrace() to pair SQL[i] with result[i].
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RepositoryResultCaptureAspect {

    private final ObjectMapper objectMapper;

    @AfterReturning(
        pointcut = "execution(* com.example.g85report.repository.*.*(..))",
        returning = "result"
    )
    public void captureResult(Object result) {
        if (!QueryResultCaptureHolder.isCapturing()) return;
        if (!(result instanceof List<?> list)) return;

        List<Map<String, Object>> rows = list.stream()
                .map(entity -> objectMapper.convertValue(
                        entity, new TypeReference<Map<String, Object>>() {}))
                .collect(Collectors.toList());

        QueryResultCaptureHolder.capture(rows);
    }
}
