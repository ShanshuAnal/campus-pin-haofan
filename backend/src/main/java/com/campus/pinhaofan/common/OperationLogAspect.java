package com.campus.pinhaofan.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int MAX_DEPTH = 4;
    private static final int MAX_COLLECTION_ITEMS = 10;
    private static final int MAX_LOG_LENGTH = 2000;
    private static final String MASK = "***";

    private final ObjectMapper objectMapper;
    private final AuthTokenUtil authTokenUtil;

    @Around("@annotation(operationLog)")
    public Object logOperation(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startNanos = System.nanoTime();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        HttpServletRequest request = currentRequest();
        Long userId = resolveUserId(request, joinPoint.getArgs());
        String requestPath = requestPath(request);
        String methodName = methodName(signature);
        String params = summarizeParameters(signature, joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long costMillis = elapsedMillis(startNanos);
            log.info(
                    "OperationLog success. operation={}, userId={}, path={}, method={}, params={}, costMs={}, result={}",
                    operationLog.value(),
                    userId,
                    requestPath,
                    methodName,
                    params,
                    costMillis,
                    toSafeJson(summarizeResult(result))
            );
            return result;
        } catch (Throwable ex) {
            long costMillis = elapsedMillis(startNanos);
            log.error(
                    "OperationLog error. operation={}, userId={}, path={}, method={}, params={}, costMs={}, exception={}: {}",
                    operationLog.value(),
                    userId,
                    requestPath,
                    methodName,
                    params,
                    costMillis,
                    ex.getClass().getSimpleName(),
                    ex.getMessage(),
                    ex
            );
            throw ex;
        }
    }

    String toSafeJson(Object value) {
        Object sanitized = sanitizeValue(value, null, 0);
        try {
            return truncate(objectMapper.writeValueAsString(sanitized));
        } catch (JsonProcessingException exception) {
            return truncate(String.valueOf(sanitized));
        }
    }

    private String summarizeParameters(MethodSignature signature, Object[] args) {
        String[] parameterNames = signature.getParameterNames();
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String parameterName = parameterNames != null && i < parameterNames.length
                    ? parameterNames[i]
                    : "arg" + i;
            parameters.put(parameterName, sanitizeValue(args[i], parameterName, 0));
        }
        return toSafeJson(parameters);
    }

    private Object summarizeResult(Object result) {
        if (result instanceof Result<?> response) {
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("code", response.getCode());
            summary.put("message", response.getMessage());
            summary.put("data", response.getData());
            return summary;
        }
        return result;
    }

    private Object sanitizeValue(Object value, String fieldName, int depth) {
        if (isSensitiveField(fieldName)) {
            return MASK;
        }
        if (value instanceof CharSequence text && isSensitiveValue(text)) {
            return MASK;
        }
        if (value == null || isSimpleValue(value)) {
            return value;
        }
        if (depth >= MAX_DEPTH) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof ServletRequest || value instanceof ServletResponse) {
            return value.getClass().getSimpleName();
        }
        if (value instanceof MultipartFile file) {
            return Map.of("name", file.getName(), "originalFilename", file.getOriginalFilename(), "size", file.getSize());
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                sanitized.put(key, sanitizeValue(entry.getValue(), key, depth + 1));
            }
            return sanitized;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>();
            int index = 0;
            for (Object item : collection) {
                if (index >= MAX_COLLECTION_ITEMS) {
                    sanitized.add("...(" + (collection.size() - MAX_COLLECTION_ITEMS) + " more)");
                    break;
                }
                sanitized.add(sanitizeValue(item, null, depth + 1));
                index++;
            }
            return sanitized;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            List<Object> sanitized = new ArrayList<>();
            for (int i = 0; i < Math.min(length, MAX_COLLECTION_ITEMS); i++) {
                sanitized.add(sanitizeValue(Array.get(value, i), null, depth + 1));
            }
            if (length > MAX_COLLECTION_ITEMS) {
                sanitized.add("...(" + (length - MAX_COLLECTION_ITEMS) + " more)");
            }
            return sanitized;
        }
        try {
            Map<String, Object> map = objectMapper.convertValue(value, new TypeReference<>() {
            });
            return sanitizeValue(map, fieldName, depth + 1);
        } catch (IllegalArgumentException exception) {
            return truncate(String.valueOf(value));
        }
    }

    private boolean isSensitiveField(String fieldName) {
        if (!StringUtils.hasText(fieldName)) {
            return false;
        }
        String normalized = fieldName.replace("-", "").replace("_", "").toLowerCase();
        return normalized.contains("password")
                || normalized.contains("token")
                || normalized.contains("secretkey")
                || normalized.contains("authorization");
    }

    private boolean isSensitiveValue(CharSequence value) {
        String text = value.toString().trim();
        return text.startsWith("Bearer ");
    }

    private boolean isSimpleValue(Object value) {
        return value instanceof CharSequence
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || value instanceof BigDecimal
                || value instanceof BigInteger
                || value instanceof TemporalAccessor;
    }

    private Long resolveUserId(HttpServletRequest request, Object[] args) {
        String authorization = request == null ? null : request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            authorization = findAuthorizationArgument(args);
        }
        if (!StringUtils.hasText(authorization)) {
            return null;
        }
        try {
            return authTokenUtil.parseUserIdFromAuthorization(authorization);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String findAuthorizationArgument(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof String value && value.startsWith("Bearer ")) {
                return value;
            }
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String requestPath(HttpServletRequest request) {
        if (request == null) {
            return "N/A";
        }
        return request.getMethod() + " " + request.getRequestURI();
    }

    private String methodName(MethodSignature signature) {
        Method method = signature.getMethod();
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= MAX_LOG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_LOG_LENGTH) + "...";
    }
}
