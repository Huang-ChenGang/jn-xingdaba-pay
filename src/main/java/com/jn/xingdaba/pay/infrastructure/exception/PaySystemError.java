package com.jn.xingdaba.pay.infrastructure.exception;

public enum PaySystemError implements PayError {
    BAD_REQUEST(400, "请求参数错误"),
    PAY_SYSTEM_ERROR(500, "支付系统异常"),
    PAY_FAILED(1000, "支付失败"),
    GET_OPEN_ID_ERROR(1100, "获取OPENID异常")
    ;

    private final int errorCode;
    private final String errorMessage;

    PaySystemError(int errorCode, String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    @Override
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
