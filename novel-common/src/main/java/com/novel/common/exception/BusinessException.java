package com.novel.common.exception;

import com.novel.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：携带自定义错误码与消息，配合 GlobalExceptionHandler 统一处理。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
