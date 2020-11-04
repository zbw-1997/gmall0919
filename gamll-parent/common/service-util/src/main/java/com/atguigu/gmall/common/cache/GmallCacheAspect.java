package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-03-21 18:41
 */
@Component
@Aspect//切面
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint joinPoint){
        Object result =null;
        //获取传递参数
        Object[] args = joinPoint.getArgs();
        //看哪个方法上有注解
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取注解
        GmallCache annotation = signature.getMethod().getAnnotation(GmallCache.class);
        //获取注解上的前缀
        String prefix = annotation.prefix();
        //定义key
        String key = prefix+ Arrays.asList(args).toString();
        //获取数据 1.需要key 2.查看在redis中的存储类型
        result=cacheHit(signature,key);
        if(result==null){
            //去数据库查询，避免缓存穿透，缓存击穿，上锁
            //定义锁
            RLock lock = redissonClient.getLock(key + ":lock");
            try {
                boolean tryLock = lock.tryLock(100, 10, TimeUnit.SECONDS);
                if(tryLock){
                    //从数据库中获取数据
                    result = joinPoint.proceed(joinPoint.getArgs());
                    if(result==null){
                        Object o = new Object();
                        redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                        return o;
                    }
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(result), RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                    return result;
                }else{
                    Thread.sleep(1000);
                    return cacheHit(signature,key);
                }

            }catch (Exception e){
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
        return result;
    }

    private Object cacheHit(MethodSignature signature, String key) {
        String o = (String) redisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(o)){
            //获取到存储的数据类型
            Class returnType = signature.getReturnType();
            //进行数据类型转换
            return JSONObject.parseObject(o,returnType);
        }
        return null;
    }
}
