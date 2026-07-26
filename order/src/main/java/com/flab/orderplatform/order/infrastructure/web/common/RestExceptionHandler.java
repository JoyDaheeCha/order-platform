package com.flab.orderplatform.order.infrastructure.web.common;

import com.flab.orderplatform.order.common.BusinessErrorCode;
import com.flab.orderplatform.order.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        var errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.name())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e) {
        var violations = e.getFieldErrors().stream()
                .map(error -> ErrorResponse.Violation
                        .builder()
                        .field(error.getField())
                        .value(String.valueOf(error.getRejectedValue()))
                        .reason(error.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse.of(BusinessErrorCode.INVALID_REQUEST, violations));
    }
}
