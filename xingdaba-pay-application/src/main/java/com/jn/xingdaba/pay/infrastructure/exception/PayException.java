package com.jn.xingdaba.pay.infrastructure.exception;

import com.jn.core.exception.JNError;
import com.jn.core.exception.JNException;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PayException extends JNException {

    public PayException(@NotNull JNError error) {
        super(error);
    }

    public PayException(@NotNull JNError error, Throwable cause) {
        super(error, cause);
    }

    public PayException(@NotNull JNError error, @NotBlank String message) {
        super(error, message);
    }
}
