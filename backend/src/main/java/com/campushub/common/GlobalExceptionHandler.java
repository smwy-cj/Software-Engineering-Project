package com.campushub.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(mapHttpStatus(e.getCode()))
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            Map<String, String> err = new HashMap<>();
            err.put("field", fe.getField());
            err.put("message", fe.getDefaultMessage());
            errors.add(err);
        }
        ApiResponse<Object> body = ApiResponse.error(40001, "参数校验失败");
        body.setErrors(errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403)
                .body(ApiResponse.error(40301, "权限不足"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneral(Exception e) {
        return ResponseEntity.status(500)
                .body(ApiResponse.error(50001, "服务器内部错误"));
    }

    private HttpStatus mapHttpStatus(int code) {
        return switch (code) {
            case 40101, 40102, 40103 -> HttpStatus.UNAUTHORIZED;
            case 40301, 40302, 40303 -> HttpStatus.FORBIDDEN;
            case 40401 -> HttpStatus.NOT_FOUND;
            case 40901 -> HttpStatus.CONFLICT;
            case 42201 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 42901 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
