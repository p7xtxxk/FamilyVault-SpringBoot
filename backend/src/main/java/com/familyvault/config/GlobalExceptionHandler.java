package com.familyvault.config;

import com.familyvault.controller.DocumentController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentController.UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(DocumentController.UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("detail", e.getMessage()));
    }

    @ExceptionHandler(DocumentController.ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(DocumentController.ForbiddenException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("detail", e.getMessage()));
    }
}
