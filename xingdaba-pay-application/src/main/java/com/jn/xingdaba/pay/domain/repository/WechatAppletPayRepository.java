package com.jn.xingdaba.pay.domain.repository;

import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface WechatAppletPayRepository extends JpaRepository<WechatAppletPay, String>, JpaSpecificationExecutor<WechatAppletPay> {
    Optional<WechatAppletPay> findByJnOrderId(String jnOrderId);
}
