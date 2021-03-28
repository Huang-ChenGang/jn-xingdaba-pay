package com.jn.xingdaba.pay.application.receiver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jn.xingdaba.order.api.UnsubscribeMessage;
import com.jn.xingdaba.pay.domain.service.WechatAppletPayDomainService;
import com.jn.xingdaba.pay.infrastructure.exception.RefundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderReceiver {
    private final ObjectMapper objectMapper;
    private final WechatAppletPayDomainService service;

    public OrderReceiver(ObjectMapper objectMapper,
                         WechatAppletPayDomainService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @RabbitListener(bindings = @QueueBinding(
            exchange = @Exchange("Unsubscribe"),
            key = "Unsubscribe",
            value = @Queue("UnsubscribeToPay")
    ))
    public void handleMessage(String message) {
        log.info("unsubscribe message from order center: {}", message);
        UnsubscribeMessage unsubscribeMessage;
        try {
            unsubscribeMessage = objectMapper.readValue(message, UnsubscribeMessage.class);
        } catch (JsonProcessingException e) {
            log.error("format message from order center error.", e);
            throw new RefundException();
        }

        service.refund(unsubscribeMessage);
    }
}
