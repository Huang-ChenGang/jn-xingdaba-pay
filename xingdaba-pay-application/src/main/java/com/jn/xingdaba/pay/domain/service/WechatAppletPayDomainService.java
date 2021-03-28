package com.jn.xingdaba.pay.domain.service;

import com.jn.xingdaba.order.api.UnsubscribeMessage;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;

public interface WechatAppletPayDomainService {
    String UNIFIED_ORDER_NOTIFY_URL = "https://api.xingdaba.com/api/pay/wechat-applet/unified-order/notify";
    String WECHAT_APPLET_TRADE_TYPE = "JSAPI";
    String WECHAT_APPLET_ID = "wxd66f31d99408b0dc";
    String MERCHANT_NO = "1602924891";
    String WECHAT_API_SECRET_KEY = "shJN20191226HjLcZyGcY7UjM6YhN5Tg";
    String WECHAT_APPLET_UNIFIED_ORDER_URL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
    String WECHAT_APPLET_REFUND_NOTIFY_URL = "https://api.xingdaba.com/api/pay/wechat-applet/refund/notify";
    String WECHAT_APPLET_REFUND_URL = "https://api.mch.weixin.qq.com/secapi/pay/refund";

    WechatAppletUnifiedOrderResponseDto unifiedOrder(WechatAppletUnifiedOrderRequestDto requestDto);

    WechatAppletPay unifiedOrderNotify(String notifyResult);

    void refund(UnsubscribeMessage unsubscribeMessage);
}
