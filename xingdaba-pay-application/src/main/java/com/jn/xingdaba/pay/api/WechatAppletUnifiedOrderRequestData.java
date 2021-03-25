package com.jn.xingdaba.pay.api;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public final class WechatAppletUnifiedOrderRequestData {

    @NotBlank(message = "微信小程序未登录")
    private String loginKey;

    @NotBlank(message = "商品说明不能为空")
    private String tradeDesc;

    @NotNull(message = "总金额不能为空")
    private BigDecimal totalAmount;

    @NotBlank(message = "订单ID不能为空")
    private String jnOrderId;

    private String couponId;
}
