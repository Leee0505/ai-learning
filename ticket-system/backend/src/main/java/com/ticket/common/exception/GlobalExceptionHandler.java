package com.ticket.common.exception;

import com.ticket.common.constant.ErrorCode;
import com.ticket.dto.response.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        R<Void> response = R.error(errorCode.getCode(), ex.getMessage());
        return ResponseEntity.status(mapHttpStatus(errorCode.getCode())).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<R<Void>> handleBadCredentials(BadCredentialsException ex) {
        R<Void> response = R.error(
                ErrorCode.INVALID_CREDENTIALS.getCode(),
                ErrorCode.INVALID_CREDENTIALS.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException ex) {
        R<Void> response = R.error(
                ErrorCode.ACCESS_DENIED.getCode(),
                ErrorCode.ACCESS_DENIED.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        R<Void> response = R.error(ErrorCode.VALIDATION_ERROR.getCode(), message);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleFallback(Exception ex) {
        log.error("Unhandled exception", ex);
        R<Void> response = R.error(
                ErrorCode.INTERNAL_ERROR.getCode(),
                ErrorCode.INTERNAL_ERROR.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private HttpStatus mapHttpStatus(int code) {
        if (code >= 40100 && code < 40200) return HttpStatus.UNAUTHORIZED;
        if (code >= 40300 && code < 40400) return HttpStatus.FORBIDDEN;
        if (code >= 40000 && code < 40100) return HttpStatus.BAD_REQUEST;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
