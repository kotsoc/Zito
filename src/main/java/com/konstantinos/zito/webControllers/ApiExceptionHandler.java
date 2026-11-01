package com.konstantinos.zito.webControllers;

import com.konstantinos.zito.model.dto.ApiError;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(org.springframework.security.access.AccessDeniedException exception) {
        return error(403);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> status(ResponseStatusException exception) {
        return error(exception.getStatusCode().value());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> conflict(OptimisticLockingFailureException exception) {
        return error(409);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> dependency(DataAccessException exception) {
        return error(503);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class})
    public ResponseEntity<ApiError> malformed(Exception exception) {
        return error(400);
    }

    @ExceptionHandler(com.konstantinos.zito.model.exceptions.MenuItemNotFoundException.class)
    public ResponseEntity<ApiError> missing(Exception exception) { return error(404); }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class,
            org.springframework.web.servlet.NoHandlerFoundException.class})
    public ResponseEntity<ApiError> missingRoute(Exception exception) { return error(404); }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> method(Exception exception) { return error(405); }

    @ExceptionHandler(org.springframework.dao.DuplicateKeyException.class)
    public ResponseEntity<ApiError> duplicate(Exception exception) { return error(409); }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiError> authentication(Exception exception) { return error(401); }

    @ExceptionHandler({java.util.concurrent.ExecutionException.class, com.google.common.util.concurrent.UncheckedExecutionException.class})
    public ResponseEntity<ApiError> cached(Exception exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof com.konstantinos.zito.model.exceptions.MenuItemNotFoundException) return error(404);
        if (cause instanceof DataAccessException) return error(503);
        return unexpected(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("Unhandled request failure", exception);
        return error(500);
    }

    private ResponseEntity<ApiError> error(int status) {
        String code = switch (status) {
            case 401 -> "UNAUTHORIZED";
            case 400 -> "INVALID_REQUEST";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 409 -> "CONFLICT";
            case 503 -> "DEPENDENCY_UNAVAILABLE";
            case 500 -> "INTERNAL_ERROR";
            default -> "REQUEST_FAILED";
        };
        String traceId = MDC.get("traceId");
        if (traceId == null) traceId = java.util.UUID.randomUUID().toString();
        return ResponseEntity.status(status).body(new ApiError(code, traceId));
    }
}
