package com.jn.xingdaba.pay.domain.service;

import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;

public interface WechatAppletPayDomainService {
    String UNIFIED_ORDER_NOTIFY_URL = "https://api.xingdaba.com/pay/wechat-applet/unified-order/notify";
    String WECHAT_APPLET_TRADE_TYPE = "JSAPI";
    String WECHAT_APPLET_ID = "wxd66f31d99408b0dc";
    String MERCHANT_NO = "1602924891";
    String WECHAT_API_SECRET_KEY = "shJN20191226HjLcZyGcY7UjM6YhN5Tg";
    String WECHAT_APPLET_UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";

    WechatAppletUnifiedOrderResponseDto unifiedOrder(WechatAppletUnifiedOrderRequestDto requestDto);
}
