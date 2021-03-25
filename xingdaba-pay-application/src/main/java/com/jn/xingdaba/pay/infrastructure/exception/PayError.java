package com.jn.xingdaba.pay.infrastructure.exception;

import com.jn.core.exception.JNError;

public interface PayError extends JNError {
    default int getServiceCode() {
        return 6;
    }
}
