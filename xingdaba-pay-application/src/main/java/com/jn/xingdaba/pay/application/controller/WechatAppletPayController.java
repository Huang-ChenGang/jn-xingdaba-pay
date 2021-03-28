package com.jn.xingdaba.pay.application.controller;

import com.jn.core.api.ServerResponse;
import com.jn.core.dto.KeyValueDto;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderRequestData;
import com.jn.xingdaba.pay.api.WechatAppletUnifiedOrderResponseData;
import com.jn.xingdaba.pay.application.service.WechatAppletPayService;
import com.jn.xingdaba.pay.infrastructure.config.XmlAssembler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

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
    public ServerResponse<String> unifiedOrderNotify(@RequestBody @NotBlank String notifyResult) {
        log.info("wechat applet unified order notify result: {}", notifyResult);
        return ServerResponse.success(service.unifiedOrderNotify(notifyResult));
    }

    @PostMapping("/refund/notify")
    public ServerResponse<String> refundNotify(@RequestBody @NotBlank String notifyResult) {
        log.info("wechat applet refund notify result: {}", notifyResult);
        // TODO 在这里将订单状态设置为已退订，发起退订时为退订中
        List<KeyValueDto> responseData = new ArrayList<>();
        responseData.add(new KeyValueDto("return_code", "SUCCESS"));
        responseData.add(new KeyValueDto("return_msg", "OK"));
        return ServerResponse.success(XmlAssembler.assembleXml(responseData));
    }
}
