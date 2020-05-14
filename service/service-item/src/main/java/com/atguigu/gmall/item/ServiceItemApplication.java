package com.atguigu.gmall.item;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author gao
 * @create 2020-04-21 21:33
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)//排除数据源
@EnableFeignClients(basePackages = {"com.atguigu.gmall"})
@EnableDiscoveryClient
@ComponentScan("com.atguigu.gmall")
public class ServiceItemApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServiceItemApplication.class,args);
    }
}
