package com.jn.xingdaba.pay.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jn.core.api.ServerResponse;
import com.jn.core.builder.KeyBuilder;
import com.jn.core.dto.KeyValueDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderRequestDto;
import com.jn.xingdaba.pay.application.dto.WechatAppletUnifiedOrderResponseDto;
import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import com.jn.xingdaba.pay.domain.repository.WechatAppletPayRepository;
import com.jn.xingdaba.pay.infrastructure.config.XmlAssembler;
import com.jn.xingdaba.pay.infrastructure.exception.PayException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.GET_OPEN_ID_ERROR;
import static com.jn.xingdaba.pay.infrastructure.exception.PaySystemError.PAY_FAILED;

@Slf4j
@Service
public class WechatAppletPayDomainServiceImpl implements WechatAppletPayDomainService {
    private final RestTemplate jnRestTemplate;
    private final ObjectMapper objectMapper;
    private final KeyBuilder keyBuilder;
    private final WechatAppletPayRepository repository;
    private final RestTemplate restTemplate;

    public WechatAppletPayDomainServiceImpl(@Qualifier("jnRestTemplate") RestTemplate jnRestTemplate,
                                            ObjectMapper objectMapper,
                                            KeyBuilder keyBuilder,
                                            WechatAppletPayRepository repository,
                                            RestTemplateBuilder restTemplateBuilder) {
        this.jnRestTemplate = jnRestTemplate;
        this.objectMapper = objectMapper;
        this.keyBuilder = keyBuilder;
        this.repository = repository;
        this.restTemplate = restTemplateBuilder.build();
    }

    @Override
    public WechatAppletUnifiedOrderResponseDto unifiedOrder(WechatAppletUnifiedOrderRequestDto requestDto) {
        // 获取参数
        String openId = getOpenId(requestDto.getCustomerId());
        String payOrderId = keyBuilder.getUniqueKey();
        String jnOrderId = requestDto.getJnOrderId();
        String payBody = requestDto.getTradeDesc();
        String payAttach = payOrderId;
        String jnPayNo = keyBuilder.getUniqueKey("JNWechatApplet");
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
        return md5Encode(toEncodeStr.toString()).toUpperCase();
    }

    private static String md5Encode(String origin) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return byteToHex(md.digest(origin.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            log.error("md5 encode error.", e);
            throw new PayException(PAY_FAILED);
        }
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
}
