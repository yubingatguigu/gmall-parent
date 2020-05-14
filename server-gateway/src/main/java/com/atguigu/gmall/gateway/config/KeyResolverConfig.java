package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author gao
 * @create 2020-05-14 16:05
 */
@Configuration
public class KeyResolverConfig {

    //按照ip限流
    @Bean
    KeyResolver ipKeyResolver() {
        System.out.println("按照ip限流-------------------------");
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

    //按照用户限流
//    @Bean
//    KeyResolver userKeyResolver() {
//        return exchange -> Mono.just(exchange.getRequest().getHeaders().get("token").get(0));
//    }

    //按照接口限流
//    @Bean
//    KeyResolver apiKeyResolver() {
//        return exchange -> Mono.just(exchange.getRequest().getPath().value());
//    }
}
