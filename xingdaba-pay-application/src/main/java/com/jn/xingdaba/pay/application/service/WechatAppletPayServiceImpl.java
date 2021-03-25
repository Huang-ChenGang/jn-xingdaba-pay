package com.jn.xingdaba.pay.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jn.core.dto.KeyValueDto;
import com.jn.xingdaba.pay.api.PaySuccessMessage;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import com.jn.xingdaba.pay.domain.service.WechatAppletPayDomainService;
import com.jn.xingdaba.pay.infrastructure.config.Md5Encoder;
import com.jn.xingdaba.pay.infrastructure.config.XmlAssembler;
import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.UNIFIED_ORDER_NOTIFY_ERROR;

@Slf4j
@Service
public class WechatAppletPayServiceImpl implements WechatAppletPayService {
    private final WechatAppletPayDomainService domainService;
    private final AmqpTemplate amqpTemplate;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public WechatAppletPayServiceImpl(WechatAppletPayDomainService domainService,
                                      AmqpTemplate amqpTemplate,
                                      ObjectMapper objectMapper, StringRedisTemplate stringRedisTemplate) {
        this.domainService = domainService;
        this.amqpTemplate = amqpTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public WechatAppletUnifiedOrderResponseData unifiedOrder(WechatAppletUnifiedOrderRequestData requestData) {
        log.info("wechat applet unified order for request data: {}", requestData);
        return WechatAppletUnifiedOrderResponseDto.toResponseData(
                domainService.unifiedOrder(WechatAppletUnifiedOrderRequestDto.fromRequestData(requestData)));
    }

    @Override
    public String unifiedOrderNotify(String notifyResult) {
        List<KeyValueDto> responseData = new ArrayList<>();
        String notifyRedisKey = "pay-success-".concat(Md5Encoder.md5Encode(notifyResult));
        Boolean alreadyProcess = stringRedisTemplate.hasKey(notifyRedisKey);
        if (alreadyProcess != null && alreadyProcess) {
            log.info("pay success notify has been processed.");
            responseData.add(new KeyValueDto("return_code", "SUCCESS"));
            responseData.add(new KeyValueDto("return_msg", "OK"));
            return XmlAssembler.assembleXml(responseData);
        }
        stringRedisTemplate.opsForValue().set(notifyRedisKey, notifyRedisKey, 25, TimeUnit.HOURS);

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
        amqpTemplate.convertAndSend("PaySuccess", "WechatApplet", message);

        responseData.add(new KeyValueDto("return_code", "SUCCESS"));
        responseData.add(new KeyValueDto("return_msg", "OK"));
        return XmlAssembler.assembleXml(responseData);
    }
}
