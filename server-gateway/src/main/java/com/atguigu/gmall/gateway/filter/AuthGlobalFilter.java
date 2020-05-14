package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;

import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author gao  网关的过滤器
 * @create 2020-04-30 0:37
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {

    //获取请求资源的列表
    @Value("${authUrls.url}")
    private String authUrls;

    //检查路径匹配对象
    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //获取用户的请求对象
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        //内部接口 /**/inner/**  不允许外部访问
        if(antPathMatcher.match("/**/inner/**",path)){
            //获取响应对象
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }

        //获取用户id
        String userId = getUserId(request);
        //获取临时用户id
        String userTempId = getUserTempId(request);
        //访问接口 /api/**/auth/** 允许访问
        if(antPathMatcher.match("/api/**/auth/**",path)){
            if(StringUtils.isEmpty(userId)){
                //获取响应对象
                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        //验证用户请求的资源url未登录的情况下不允许访问
        if(null != authUrls){
            //循环判断
            for (String authUrl : authUrls.split(",")) {
                //判断path中是否包含以上请求，如果有但未登录，提示登录
                if(path.indexOf(authUrl)!=-1 && StringUtils.isEmpty(userId)){
                    //获取响应对象
                    ServerHttpResponse response = exchange.getResponse();
                    //赋值一个状态码  303 请求对应的资源，存在另一个url，重定向
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    //重定向到登录链接
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                    //重定向到登录
                    return response.setComplete();
                }
            }
        }

        //上述验证通过，需要将userId，传递到各个微服务上  如果用户没有登陆的时候，添加购物车的时候会产生一个临时的用户id，传递给各个微服务
        if(!StringUtils.isEmpty(userId) || !StringUtils.isEmpty(userTempId)){
            //传递登录的用户id
            if(!StringUtils.isEmpty(userId)){
                //存储一个userId
                request.mutate().header("userId",userId);
            }
            //传递临时的用户id
            if(!StringUtils.isEmpty(userTempId)){
                //存储一个userTempId
                request.mutate().header("userTempId",userTempId);
            }
            //将userId传递下去
            return chain.filter(exchange.mutate().request(request).build());
        }

        return chain.filter(exchange);
    }

    //提示信息告诉用户，提示信息封装到resultCodeEnum枚举对象中
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        //将提示信息封装到result中
        Result<Object> result = Result.build(null, resultCodeEnum);
        //将result转换为字符串
        String resultStr = JSONObject.toJSONString(result);
        //将resultStr转换为一个字节数组
        byte[] bytes = resultStr.getBytes(StandardCharsets.UTF_8);
        //声明一个databuffer
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        //设置输入格式
        response.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        //将信息输入到页面
        return response.writeWith(Mono.just(wrap));
    }

    //获取userId
    private String getUserId(ServerHttpRequest request) {
        //用户id 存储在缓存       key=user:login:token value=userId  先获取到token，token从在cookie，header中
        String token = "";
        List<String> tokenList = request.getHeaders().get("token");
        if(null!=tokenList && tokenList.size()>0){
            //这个集合中只有一个key，这个key只有一个值
            token = tokenList.get(0);
        }else {
            MultiValueMap<String, HttpCookie> cookies = request.getCookies();
            //根据cookie的key来获取数据，
            HttpCookie cookie = cookies.getFirst("token");
            if(null!=cookie){
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        if(!StringUtils.isEmpty(token)){
            //才能从缓存中获取数据
            String userKey = "user:login:"+token;
            String userId = (String) redisTemplate.opsForValue().get(userKey);
            return userId;
        }
        return "";
    }

    //在网关中获取临时用户id，用户在添加购物车中必然走网关
    private String getUserTempId (ServerHttpRequest request){
        String userTempId = "";
        List<String> userTempIdList = request.getHeaders().get("userTempId");
        if(null != userTempIdList){
            userTempId = userTempIdList.get(0);
        }else {
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if(null != cookie){
                userTempId = URLDecoder.decode(cookie.getValue());
            }
        }
        return userTempId;
    }

}
