package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-18 11:50
 */
@FeignClient(name = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {

    @GetMapping("/api/item/{skuId}")
    public Result getSkuInfoBySkuId(@PathVariable Long skuId);
}
