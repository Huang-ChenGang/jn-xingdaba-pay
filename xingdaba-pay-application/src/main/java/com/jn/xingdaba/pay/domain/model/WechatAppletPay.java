package com.jn.xingdaba.pay.domain.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
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

    private String createBy;

    @CreatedDate
    private LocalDateTime createTime;

    private String updateBy;

    @LastModifiedDate
    private LocalDateTime updateTime;
}
