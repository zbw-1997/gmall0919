package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-17 14:47
 */
public interface ItemService {
    //封装商品详情的所有信息
    Map<String,Object> getSkuInfoBySkuId(Long skuId);
}
