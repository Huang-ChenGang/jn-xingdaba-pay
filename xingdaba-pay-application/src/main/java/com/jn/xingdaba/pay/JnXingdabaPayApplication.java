package com.jn.xingdaba.pay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.jn.*"})
@SpringBootApplication
public class JnXingdabaPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(JnXingdabaPayApplication.class, args);
    }

}
