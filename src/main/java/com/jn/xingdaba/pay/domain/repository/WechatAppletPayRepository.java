package com.jn.xingdaba.pay.domain.repository;

import com.jn.xingdaba.pay.domain.model.WechatAppletPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WechatAppletPayRepository extends JpaRepository<WechatAppletPay, String>, JpaSpecificationExecutor<WechatAppletPay> {
}
