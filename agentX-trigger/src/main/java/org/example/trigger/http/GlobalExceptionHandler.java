package org.example.trigger.http;

import org.example.api.response.Response;
import org.example.types.enums.ResponseCode;
import org.example.types.exception.AppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一将领域/框架异常转换为协议层统一响应，避免异常以 500 默认错误直接暴露给前端。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常：领域层抛出的 AppException，直接透传 code + info
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<Response<Void>> handleAppException(AppException e) {
        log.warn("业务异常: code={}, info={}", e.getCode(), e.getInfo());
        HttpStatus status = HttpStatus.OK;
        if (ResponseCode.UNAUTHORIZED.getCode().equals(e.getCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ResponseCode.FORBIDDEN.getCode().equals(e.getCode())) {
            status = HttpStatus.FORBIDDEN;
        } else if (ResponseCode.TOKEN_LIMIT_EXCEEDED.getCode().equals(e.getCode())) {
            status = HttpStatus.TOO_MANY_REQUESTS;
        }
        return ResponseEntity.status(status).body(Response.error(e.getCode(), e.getInfo()));
    }

    /**
     * 入参校验异常：@Validated + 注解校验失败时抛出
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Response<Void> handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse(ResponseCode.ILLEGAL_PARAMETER.getInfo());
        log.warn("参数校验失败: {}", message);
        return Response.error(ResponseCode.ILLEGAL_PARAMETER.getCode(), message);
    }

    /**
     * 兜底异常：未预期的异常，统一返回未知失败并记录堆栈
     */
    @ExceptionHandler(Exception.class)
    public Response<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Response.error(ResponseCode.UN_ERROR);
    }
}
