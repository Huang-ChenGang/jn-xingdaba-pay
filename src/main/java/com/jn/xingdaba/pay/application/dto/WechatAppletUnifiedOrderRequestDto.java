package com.jn.xingdaba.pay.application.dto;

import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

@Data
public final class WechatAppletUnifiedOrderRequestDto {
    private String customerId;

    private String tradeDesc;

    private BigDecimal totalAmount;

    private String jnOrderId;

    private String couponId;

    public static WechatAppletUnifiedOrderRequestDto fromRequestData(WechatAppletUnifiedOrderRequestData requestData) {
        WechatAppletUnifiedOrderRequestDto requestDto = new WechatAppletUnifiedOrderRequestDto();
        BeanUtils.copyProperties(requestData, requestDto);
        requestDto.setCustomerId(requestData.getLoginKey());
        return requestDto;
    }
}
