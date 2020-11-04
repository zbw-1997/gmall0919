package com.atguigu.gmall.item.client.impl;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.ItemFeignClient;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-18 11:51
 */
@Component
public class ItemDegradeFeignClient implements ItemFeignClient {
    @Override
    public Result getSkuInfoBySkuId(Long skuId) {
        return Result.fail();
    }
}
