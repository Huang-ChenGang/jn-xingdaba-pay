package com.jn.xingdaba.pay.infrastructure.config;

import com.jn.xingdaba.pay.infrastructure.exception.RefundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

@Slf4j
@Component
public class JnRestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate jnRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestTemplate wechatRestTemplate() {
        RestTemplate restTemplate;

        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            InputStream stream = getClass().getClassLoader().getResourceAsStream("cert/apiclient_cert.p12");
            keyStore.load(stream, "1602924891".toCharArray());

            // Trust own CA and all self-signed certs
            SSLContext sslcontext = SSLContextBuilder.create()
                    .loadKeyMaterial(keyStore, "1602924891".toCharArray())
                    .build();

            // Allow TLSv1 protocol only
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, new String[]{"TLSv1"}, null, NoopHostnameVerifier.INSTANCE);
            CloseableHttpClient httpClient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
            restTemplate = new RestTemplate(factory);

            // 将转换器的编码换成utf-8
            restTemplate.getMessageConverters().set(1, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("load wechatRestTemplate error.", e);
            throw new RefundException();
        }

        return restTemplate;
    }
}
