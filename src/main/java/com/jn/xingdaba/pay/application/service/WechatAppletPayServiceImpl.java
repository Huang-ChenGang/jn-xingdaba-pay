package com.jn.xingdaba.pay.application.service;

import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import com.jn.xingdaba.pay.domain.service.WechatAppletPayDomainService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class WechatAppletPayServiceImpl implements WechatAppletPayService {
    private final WechatAppletPayDomainService domainService;
    private final AmqpTemplate amqpTemplate;

    public WechatAppletPayServiceImpl(WechatAppletPayDomainService domainService,
                                      AmqpTemplate amqpTemplate) {
        this.domainService = domainService;
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public WechatAppletUnifiedOrderResponseData unifiedOrder(WechatAppletUnifiedOrderRequestData requestData) {
        log.info("wechat applet unified order for request data: {}", requestData);
        return WechatAppletUnifiedOrderResponseDto.toResponseData(
                domainService.unifiedOrder(WechatAppletUnifiedOrderRequestDto.fromRequestData(requestData)));
    }

    @Override
    public void unifiedOrderNotify(String notifyResult) {
        log.info("unified order notify result: {}", notifyResult);
        WechatAppletPay wechatAppletPay = domainService.unifiedOrderNotify(notifyResult);
    }
}
