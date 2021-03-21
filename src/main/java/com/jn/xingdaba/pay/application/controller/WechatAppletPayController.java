package com.jn.xingdaba.pay.application.controller;

import com.jn.core.api.ServerResponse;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import com.jn.xingdaba.pay.application.service.WechatAppletPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotNull;

@Slf4j
@Validated
@RestController
@RequestMapping("/wechat-applet")
public class WechatAppletPayController {
    private final WechatAppletPayService service;

    public WechatAppletPayController(WechatAppletPayService service) {
        this.service = service;
    }

    @PostMapping("/unified-order")
    public ServerResponse<WechatAppletUnifiedOrderResponseData> unifiedOrder(@RequestBody @NotNull @Validated WechatAppletUnifiedOrderRequestData requestData) {
        return ServerResponse.success(service.unifiedOrder(requestData));
    }

    @PostMapping("/unified-order/notify")
    public ServerResponse<Void> unifiedOrderNotify(@RequestBody String notifyResult) {
        log.info("wechat applet unified order notify result: {}", notifyResult);
        return ServerResponse.success();
    }
}
