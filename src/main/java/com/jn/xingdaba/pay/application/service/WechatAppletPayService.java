package com.jn.xingdaba.pay.application.service;

import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;

public interface WechatAppletPayService {
    WechatAppletUnifiedOrderResponseData unifiedOrder(WechatAppletUnifiedOrderRequestData requestData);
}
