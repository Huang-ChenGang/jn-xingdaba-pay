package com.jn.xingdaba.pay.api;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public final class PaySuccessMessage {
    private String orderId;
    private LocalDateTime payTime;
    private BigDecimal realAmount;
    private String couponId;
}
