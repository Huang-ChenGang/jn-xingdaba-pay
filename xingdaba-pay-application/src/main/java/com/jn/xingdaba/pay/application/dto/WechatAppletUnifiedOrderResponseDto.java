package com.jn.xingdaba.pay.application.dto;

import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public final class WechatAppletUnifiedOrderResponseDto {

    /** 时间戳 **/
    private String timeStamp;

    /** 随机字符串 **/
    private String nonceStr;

    /** 预支付ID **/
    private String prepayId;

    /** 签名算法类型 **/
    private String signType;

    /** 签名 **/
    private String paySign;

    public static WechatAppletUnifiedOrderResponseData toResponseData(WechatAppletUnifiedOrderResponseDto dto) {
        WechatAppletUnifiedOrderResponseData responseData = new WechatAppletUnifiedOrderResponseData();
        BeanUtils.copyProperties(dto, responseData);
        return responseData;
    }
}
