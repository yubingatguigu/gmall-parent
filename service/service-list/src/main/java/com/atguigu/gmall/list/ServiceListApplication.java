package com.atguigu.gmall.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author gao
 * @create 2020-04-27 20:03
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
@ComponentScan({"com.atguigu.gmall"})
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
public class ServiceListApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceListApplication.class,args);
    }
}