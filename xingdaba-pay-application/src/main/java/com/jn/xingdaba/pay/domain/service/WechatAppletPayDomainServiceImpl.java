package com.jn.xingdaba.pay.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jn.core.api.ServerResponse;
import com.jn.core.builder.KeyBuilder;
import com.jn.core.dto.KeyValueDto;
import com.jn.xingdaba.order.api.UnsubscribeMessage;
import com.jn.xingdaba.order.api.UserOrderMessage;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import com.jn.xingdaba.pay.domain.repository.WechatAppletPayRepository;
import com.jn.xingdaba.pay.infrastructure.config.Md5Encoder;
import com.jn.xingdaba.pay.infrastructure.config.XmlAssembler;
import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import com.jn.xingdaba.pay.infrastructure.exception.RefundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.*;

@Slf4j
@Service
public class WechatAppletPayDomainServiceImpl implements WechatAppletPayDomainService {
    private final RestTemplate jnRestTemplate;
    private final ObjectMapper objectMapper;
    private final KeyBuilder keyBuilder;
    private final WechatAppletPayRepository repository;
    private final RestTemplate restTemplate;
    private final RestTemplate wechatRestTemplate;
    private final AmqpTemplate amqpTemplate;

    public WechatAppletPayDomainServiceImpl(@Qualifier("jnRestTemplate") RestTemplate jnRestTemplate,
                                            ObjectMapper objectMapper,
                                            KeyBuilder keyBuilder,
                                            WechatAppletPayRepository repository,
                                            RestTemplateBuilder restTemplateBuilder,
                                            RestTemplate wechatRestTemplate,
                                            AmqpTemplate amqpTemplate) {
        this.jnRestTemplate = jnRestTemplate;
        this.objectMapper = objectMapper;
        this.keyBuilder = keyBuilder;
        this.repository = repository;
        this.restTemplate = restTemplateBuilder.build();
        this.wechatRestTemplate = wechatRestTemplate;
        this.amqpTemplate = amqpTemplate;
    }

    @Override
    public WechatAppletUnifiedOrderResponseDto unifiedOrder(WechatAppletUnifiedOrderRequestDto requestDto) {
        // 获取参数
        String openId = getOpenId(requestDto.getCustomerId());
        String payOrderId = keyBuilder.getUniqueKey();
        String jnOrderId = requestDto.getJnOrderId();
        String payBody = requestDto.getTradeDesc();
        String payAttach = payOrderId;
        String jnPayNo = keyBuilder.getUniqueKey("JXWP");
        BigDecimal orderAmount = requestDto.getTotalAmount();
        String payIp;
        try {
            payIp = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("get pay server ip error.", e);
            throw new PayException(GET_OPEN_ID_ERROR, e.getMessage());
        }
        String notifyUrl = UNIFIED_ORDER_NOTIFY_URL;
        String wechatTradeType = WECHAT_APPLET_TRADE_TYPE;

        // 拼接下单参数
        SortedMap<String, String> parameterMap = new TreeMap<>();
        parameterMap.put("appid", WECHAT_APPLET_ID);
        parameterMap.put("mch_id", MERCHANT_NO);
        parameterMap.put("nonce_str", keyBuilder.getUniqueKey());
        parameterMap.put("openid", openId);
        parameterMap.put("body", payBody);
        parameterMap.put("attach", payAttach);
        parameterMap.put("out_trade_no", jnPayNo);
        parameterMap.put("total_fee", yuanToFen(orderAmount).toString());
        parameterMap.put("spbill_create_ip", payIp);
        parameterMap.put("notify_url", notifyUrl);
        parameterMap.put("trade_type", wechatTradeType);

        // 订单入库
        WechatAppletPay wechatAppletPay = new WechatAppletPay();
        wechatAppletPay.setId(payOrderId);
        wechatAppletPay.setJnOrderId(jnOrderId);
        wechatAppletPay.setJnPayNo(jnPayNo);
        wechatAppletPay.setPayOpenId(openId);
        wechatAppletPay.setWechatTradeType(wechatTradeType);
        wechatAppletPay.setPayBody(payBody);
        wechatAppletPay.setPayAttach(payAttach);
        wechatAppletPay.setOrderAmount(orderAmount);
        wechatAppletPay.setRealAmount(BigDecimal.ZERO);
        wechatAppletPay.setPayIp(payIp);
        wechatAppletPay.setNotifyUrl(notifyUrl);
        wechatAppletPay.setPayState("UNPAID");
        wechatAppletPay.setCouponId(requestDto.getCouponId());
        repository.save(wechatAppletPay);

        // 组装Xml字符串
        List<KeyValueDto> paramList = parameterMap.entrySet().stream()
                .map(e -> new KeyValueDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        paramList.add(new KeyValueDto("sign", getApiSign(parameterMap)));

        String xmlParam = XmlAssembler.assembleXml(paramList);
        log.info("wechat unified order param: {}", xmlParam);

        HttpEntity<String> requestEntity = new HttpEntity<>(xmlParam);
        String wechatUnifiedOrderResponseJson = restTemplate.postForObject(WECHAT_APPLET_UNIFIED_ORDER_URL, requestEntity, String.class);
        log.info("wechat unified order response json: {}", wechatUnifiedOrderResponseJson);

        // 解析Xml
        Map<String, String> wechatUnifiedOrderResponse = XmlAssembler.analysisXml(wechatUnifiedOrderResponseJson);
        if (wechatUnifiedOrderResponse == null) {
            throw new PayException(GET_OPEN_ID_ERROR);
        }

        // 设置返回值
        String retTimeStamp = String.valueOf((LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) / 1000L);
        String retNonceStr = keyBuilder.getUniqueKey();
        String retPrepayId = "prepay_id=".concat(wechatUnifiedOrderResponse.get("prepay_id"));
        parameterMap = new TreeMap<>();
        parameterMap.put("appId", WECHAT_APPLET_ID);
        parameterMap.put("timeStamp", retTimeStamp);
        parameterMap.put("nonceStr", retNonceStr);
        parameterMap.put("package", retPrepayId);
        parameterMap.put("signType", "MD5");

        WechatAppletUnifiedOrderResponseDto responseDto = new WechatAppletUnifiedOrderResponseDto();
        responseDto.setTimeStamp(retTimeStamp);
        responseDto.setNonceStr(retNonceStr);
        responseDto.setPrepayId(retPrepayId);
        responseDto.setSignType("MD5");
        responseDto.setPaySign(getApiSign(parameterMap));
        return responseDto;
    }

    @Override
    public WechatAppletPay unifiedOrderNotify(String notifyResult) {
        Map<String, String> wechatNotifyResult = XmlAssembler.analysisXml(notifyResult);

        if (wechatNotifyResult == null) {
            throw new PayException(UNIFIED_ORDER_NOTIFY_ERROR);
        }

        if (!"SUCCESS".equals(wechatNotifyResult.get("result_code"))
                || !"SUCCESS".equals(wechatNotifyResult.get("return_code"))) {
            // TODO 支付失败逻辑处理 异常返回按照微信要求的格式
            log.info("pay failed");
            log.info("result_code = [" + wechatNotifyResult.get("result_code") + "]");
            log.info("return_code = [" + wechatNotifyResult.get("return_code") + "]");
            return null;
        }

        // TODO 添加金额验证，异常返回按照微信要求的格式

        // 获取支付结果参数
        String payOrderId = wechatNotifyResult.get("attach");
        BigDecimal realAmount = BigDecimal.valueOf(Integer.parseInt(wechatNotifyResult.get("total_fee"))).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        String wechatPayNo = wechatNotifyResult.get("transaction_id");

        // 更新支付订单信息
        WechatAppletPay wechatAppletPay = repository.findById(payOrderId).orElseThrow(PayException::new);
        wechatAppletPay.setRealAmount(realAmount);
        wechatAppletPay.setWechatPayNo(wechatPayNo);
        wechatAppletPay.setWechatResultMsg(notifyResult);
        wechatAppletPay.setPayState("PAID");
        wechatAppletPay.setPayTime(LocalDateTime.now());
        return repository.save(wechatAppletPay);
    }

    @Override
    public void refund(UnsubscribeMessage unsubscribeMessage) {
        log.info("refund for unsubscribe message: {}", unsubscribeMessage);
        WechatAppletPay wechatAppletPay = repository.findByJnOrderId(unsubscribeMessage.getJnOrderId()).orElseThrow(RefundException::new);

        // TODO 订单金额小于退款金额验证

        // 拼接退款参数
        SortedMap<String, String> paramMap = new TreeMap();
        paramMap.put("appid", WECHAT_APPLET_ID);
        paramMap.put("mch_id", MERCHANT_NO);
        paramMap.put("nonce_str", keyBuilder.getUniqueKey());
        paramMap.put("transaction_id", wechatAppletPay.getWechatPayNo());
        paramMap.put("out_refund_no", keyBuilder.getUniqueKey());
        paramMap.put("total_fee", yuanToFen(wechatAppletPay.getRealAmount()).toString());
        paramMap.put("refund_fee", yuanToFen(unsubscribeMessage.getRefundAmount()).toString());
        paramMap.put("notify_url", WECHAT_APPLET_REFUND_NOTIFY_URL);

        // 获取签名
        String sign = getApiSign(paramMap);

        // 组装Xml字符串
        List<KeyValueDto> paramList = new ArrayList<>();
        for (Map.Entry<String, String> param : paramMap.entrySet()) {
            paramList.add(new KeyValueDto(param.getKey(), param.getValue()));
        }
        paramList.add(new KeyValueDto("sign", sign));
        String xmlParam = XmlAssembler.assembleXml(paramList);

        HttpEntity<String> requestEntity = new HttpEntity<>(xmlParam);
        String wechatRefundResponseJson = wechatRestTemplate.postForObject(WECHAT_APPLET_REFUND_URL, requestEntity, String.class);
        log.info("wechat refund response json: {}", wechatRefundResponseJson);

        // 解析Xml
        Map<String, String> wechatRefundResponse = XmlAssembler.analysisXml(wechatRefundResponseJson);
        if (wechatRefundResponse == null) {
            throw new RefundException();
        }

        // 退款成功
        if ("SUCCESS".equals(wechatRefundResponse.get("return_code")) && "SUCCESS".equals(wechatRefundResponse.get("result_code"))) {
            wechatAppletPay.setPayState("REFUND");
            repository.save(wechatAppletPay);

            UserOrderMessage orderMessage = getUserOrderMessage(unsubscribeMessage.getJnOrderId());
            String message;
            try {
                message = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(orderMessage);
            } catch (JsonProcessingException e) {
                log.error("order message to json error.", e);
                throw new PayException(UNIFIED_ORDER_NOTIFY_ERROR);
            }
            amqpTemplate.convertAndSend("RefundSuccess", "WechatApplet", message);
        } else {
            // TODO 退款失败逻辑
        }
    }

    private UserOrderMessage getUserOrderMessage(String orderId) {
        String getUrl = "http://XINGDABA-ORDER/orders/order-message/{orderId}";
        String resourceResponseJson = jnRestTemplate.getForObject(getUrl, String.class, orderId);
        ServerResponse<UserOrderMessage> resourceResponse;
        try {
            resourceResponse = objectMapper.readValue(resourceResponseJson, new TypeReference<ServerResponse<UserOrderMessage>>(){});
        } catch (JsonProcessingException e) {
            log.error("get order message from order server error.", e);
            throw new PayException(GET_ORDER_MESSAGE_ERROR, e.getMessage());
        }
        if (!"0".equals(resourceResponse.getCode())) {
            throw new PayException(GET_ORDER_MESSAGE_ERROR, resourceResponse.getMessage());
        }
        return resourceResponse.getData();
    }

    private String getOpenId(String customerId) {
        String getUrl = "http://XINGDABA-CUSTOMER/wechat/applet/customers/open-id/{customerId}";
        String resourceResponseJson = jnRestTemplate.getForObject(getUrl, String.class, customerId);
        ServerResponse<String> resourceResponse;
        try {
            resourceResponse = objectMapper.readValue(resourceResponseJson, new TypeReference<ServerResponse<String>>(){});
        } catch (JsonProcessingException e) {
            log.error("get open id from customer server error.", e);
            throw new PayException(GET_OPEN_ID_ERROR, e.getMessage());
        }
        if (!"0".equals(resourceResponse.getCode())) {
            throw new PayException(GET_OPEN_ID_ERROR, resourceResponse.getMessage());
        }
        return resourceResponse.getData();
    }

    private BigDecimal yuanToFen(BigDecimal amount){
        return amount.multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_UP);
    }

    public static String getApiSign(SortedMap<String, String> paramList) {
        StringBuilder toEncodeStr = new StringBuilder();

        for (Map.Entry<String, String> param : paramList.entrySet()) {
            if (StringUtils.isBlank(param.getKey()) || StringUtils.isBlank(param.getValue()))  {
                continue;
            }
            if ("key".equals(param.getKey())) {
                continue;
            }

            toEncodeStr.append(param.getKey().concat("=").concat(param.getValue()).concat("&"));
        }

        toEncodeStr.append("key=" + WECHAT_API_SECRET_KEY);
        return Md5Encoder.md5Encode(toEncodeStr.toString()).toUpperCase();
    }

}
