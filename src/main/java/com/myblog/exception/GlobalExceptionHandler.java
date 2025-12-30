package com.myblog.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        // Если это запрос к комментариям с undefined, возвращаем пустой массив
        // чтобы фронтенд не падал с ошибкой .map()
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // Проверяем, что это GET запрос к комментариям
        if (requestURI != null && requestURI.contains("/comments") && "GET".equals(method)) {
            // Возвращаем пустой массив для фронтенда
            return ResponseEntity.ok(new ArrayList<>());
        }
        
        // Для остальных случаев возвращаем объект ошибки
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid parameter: " + ex.getName());
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<Map<String, String>> handleNumberFormat(NumberFormatException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Invalid number format");
        error.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

