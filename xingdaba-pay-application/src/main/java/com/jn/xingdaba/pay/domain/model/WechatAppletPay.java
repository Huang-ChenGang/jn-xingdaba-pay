package com.jn.xingdaba.pay.domain.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
public class WechatAppletPay {
    @Id
    private String id;

    private String jnOrderId;

    private String jnPayNo;

    private String wechatPayNo;

    private String payOpenId;

    private String wechatTradeType;

    private String payBody;

    private String payAttach;

    private BigDecimal orderAmount;

    private BigDecimal realAmount;

    private String payIp;

    private String notifyUrl;

    private String wechatResultMsg;

    private String payState;

    private LocalDateTime payTime;

    private String couponId;

    private String isDelete;

    @CreatedDate
    private String createBy;

    private LocalDateTime createTime;

    @LastModifiedDate
    private String updateBy;

    private LocalDateTime updateTime;
}
