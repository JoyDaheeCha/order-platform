package com.flab.orderplatform.order.infrastructure.web.common;

import com.flab.orderplatform.order.application.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.flab.orderplatform.order.infrastructure.web.common.ErrorCode.NOT_FOUND;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handle(BusinessException e) {
        var errorCode = NOT_FOUND;
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
                        ErrorResponse.of(ErrorCode.INVALID_REQUEST, violations));
    }
}
