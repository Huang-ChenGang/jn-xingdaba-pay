package com.jn.xingdaba.pay.infrastructure.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class JnRestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate jnRestTemplate() {
        return new RestTemplate();
    }
}
