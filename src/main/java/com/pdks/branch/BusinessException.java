package com.pdks.branch;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusiness(BusinessException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error",     ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
            "error",     "Sunucu hatası: " + ex.getMessage(),
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
