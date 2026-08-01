package com.novel.common.exception;

import com.novel.common.result.Result;
import com.novel.common.result.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一捕获并转换为标准 Result 响应，
 * 避免各 Controller 中重复的 try-catch 与错误信息拼接。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 参数校验失败（@Validated / @NotNull 等）
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidException(Exception e) {
        String msg;
        if (e instanceof MethodArgumentNotValidException) {
            msg = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors().stream()
                    .map(fe -> fe.getField() + ":" + fe.getDefaultMessage())
                    .collect(Collectors.joining("; "));
        } else {
            msg = ((BindException) e).getBindingResult().getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        log.warn("参数校验失败: {}", msg);
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        String msg = "缺少必填参数: " + e.getParameterName();
        log.warn(msg);
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), msg);
    }

    /**
     * 业务层抛出的自定义异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 空指针等未预期异常统一兜底
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error(ResultCode.INTERNAL_SERVER_ERROR.getCode(), "服务器内部错误，请稍后重试");
    }
}
