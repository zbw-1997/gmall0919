package com.atguigu.gmall.activity.client.impl;

import com.atguigu.gmall.activity.client.ActivityFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-04-06 13:01
 */
@Component
public class ActivityDegradeFeignClient implements ActivityFeignClient {
    @Override
    public Result findAll() {
        return null;
    }

    @Override
    public Result getSeckillGoods(Long skuId) {
        return null;
    }

    @Override
    public Result<Map<String, Object>> trade() {
        return null;
    }
}
