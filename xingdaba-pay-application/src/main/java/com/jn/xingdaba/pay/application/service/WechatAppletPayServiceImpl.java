package com.jn.xingdaba.pay.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jn.xingdaba.pay.api.PaySuccessMessage;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import com.jn.xingdaba.pay.domain.service.WechatAppletPayDomainService;
import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.UNIFIED_ORDER_NOTIFY_ERROR;

@Slf4j
@Service
public class WechatAppletPayServiceImpl implements WechatAppletPayService {
    private final WechatAppletPayDomainService domainService;
    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;

    public WechatAppletPayServiceImpl(WechatAppletPayDomainService domainService,
                                      AmqpTemplate amqpTemplate,
                                      ObjectMapper objectMapper) {
        this.domainService = domainService;
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
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
        PaySuccessMessage paySuccessMessage = new PaySuccessMessage();
        paySuccessMessage.setOrderId(wechatAppletPay.getJnOrderId());
        paySuccessMessage.setPayTime(wechatAppletPay.getPayTime());
        paySuccessMessage.setRealAmount(wechatAppletPay.getRealAmount());
        paySuccessMessage.setCouponId(wechatAppletPay.getCouponId());

        String message;
        try {
            message = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(paySuccessMessage);
        } catch (JsonProcessingException e) {
            log.error("format pay success message to json error.", e);
            throw new PayException(UNIFIED_ORDER_NOTIFY_ERROR);
        }
        amqpTemplate.convertAndSend("paySuccess", "order", message);
    }
}
