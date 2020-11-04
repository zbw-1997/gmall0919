package com.atguigu.gamll.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-25 21:06
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {
    //引入匹配路径的类
    private AntPathMatcher antPathMatcher =new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     *
     * @param exchange 能够获取请求路径
     * @param chain 过滤链
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1.知道用户的请求的地址
        //拿到request对象
        ServerHttpRequest request = exchange.getRequest();
        //获取请求地址的路径
        String path = request.getURI().getPath();
        //2.验证请求地址是否是内部地址，若是内部地址，没有访问权限 内部地址是/**/inner/**
        if(!StringUtils.isEmpty(path)){
            if(antPathMatcher.match("/**/inner/**",path)){
                //若路径匹配成功是内部的地址
                //得到相应
                ServerHttpResponse response = exchange.getResponse();
                //返回没有权限
                return out(response, ResultCodeEnum.PERMISSION);
            }
        }
        //3.获取用户信息
        //用户信息在缓存中
        String userId = getUser(request);
        //4.验证用户是否登录  然后对api异步请求或者web同步请求(控制器)进行认证
        if(antPathMatcher.match("/api/**/auth/**",path)){
            if(StringUtils.isEmpty(userId)){
                //若路径匹配成功是内部的地址且用户没有登录
                //得到相应
                ServerHttpResponse response = exchange.getResponse();
                //返回没有权限
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }
        //验证控制器
        if(!StringUtils.isEmpty(authUrls)){
            //循环判断
            for (String authUrl : authUrls.split(",")) {
                if(path.indexOf(authUrl)!=-1 && StringUtils.isEmpty(userId)){
                    ServerHttpResponse response = exchange.getResponse();
                    //表示存在url，但是需要登录，重定向到登录页面
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                    return response.setComplete();
                }
            }
        }
        String userTempId = getTempId(request);
        //讲用户信息传递到其他业务模块
        if(!StringUtils.isEmpty(userId)||!StringUtils.isEmpty(userTempId)){
            if(!StringUtils.isEmpty(userId)) {
                request.mutate().header("userId", userId).build();
            }
            if(!StringUtils.isEmpty(userTempId)) {
                request.mutate().header("userTempId", userTempId).build();
            }
            ServerWebExchange build = exchange.mutate().request(request).build();
            return chain.filter(build);
        }
        return chain.filter(exchange);
    }
    //获取临时用户id
    public String getTempId(ServerHttpRequest request){
        String tempId ="";
        //从header中获取
        List<String> list = request.getHeaders().get("userTempId");
        if(!CollectionUtils.isEmpty(list)){
            tempId =list.get(0);
        }else{
            //从cookie中获取
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if(cookie!=null){
                tempId =cookie.getValue();
            }else{
                return "";
            }
        }
        return tempId;
    }
    //获取用户id
    private String getUser(ServerHttpRequest request) {
        String token="";
        // 从header 中获取token数据
        List<String> tokenList = request.getHeaders().get("token");
        if (!CollectionUtils.isEmpty(tokenList)){
            token = tokenList.get(0);
        }else {//从cookie中获取token
            MultiValueMap<String, HttpCookie> cookieMultiValueMap  = request.getCookies();
            HttpCookie cookie = cookieMultiValueMap.getFirst("token");
            if (cookie!=null){
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        // 如果获取到了token 则从缓存中获取到用户Id
        if(!StringUtils.isEmpty(token)) {
            String userId = (String) redisTemplate.opsForValue().get("user:login:" + token);
            return userId;
        }
        return "";
    }
    //没有权限
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum permission) {
        // 返回用户没有权限登录
        Result<Object> result = Result.build(null, permission);
        byte[] bits = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer wrap = response.bufferFactory().wrap(bits);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        // 输入到页面
        return response.writeWith(Mono.just(wrap));
    }
}
