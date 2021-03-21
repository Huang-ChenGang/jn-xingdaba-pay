package com.jn.xingdaba.pay.api;

import lombok.Data;

@Data
public final class WechatAppletUnifiedOrderResponseData {

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
}
