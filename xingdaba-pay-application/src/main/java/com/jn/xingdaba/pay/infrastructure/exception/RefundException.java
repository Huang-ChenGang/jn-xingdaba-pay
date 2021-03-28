package com.jn.xingdaba.pay.infrastructure.exception;

import com.jn.core.exception.JNError;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.REFUND_ERROR;

public class RefundException extends PayException {
    public RefundException() {
        this(REFUND_ERROR);
    }

    public RefundException(@NotNull JNError error) {
        super(error);
    }

    public RefundException(@NotNull JNError error, Throwable cause) {
        super(error, cause);
    }

    public RefundException(@NotNull JNError error, @NotBlank String message) {
        super(error, message);
    }
}
