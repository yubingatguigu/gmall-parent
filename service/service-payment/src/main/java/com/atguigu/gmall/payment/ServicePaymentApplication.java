package com.atguigu.gmall.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

import javax.xml.ws.Service;

/**
 * @author gao
 * @create 2020-05-08 19:36
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
public class ServicePaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServicePaymentApplication.class,args);
    }
}
