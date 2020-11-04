package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Administrator
 * @create 2020-03-17 14:50
 */
@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private ListFeignClient listFeignClient;
    @Override
    public Map<String, Object> getSkuInfoBySkuId(Long skuId) {
        Map<String,Object> resultMap =new HashMap<>();
        //sku基本信息
        CompletableFuture<SkuInfo> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            resultMap.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);
        //获取价格数据
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            resultMap.put("price", skuPrice);
        }, threadPoolExecutor);
        //查询分类信息
        CompletableFuture<Void> categoryViewCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            resultMap.put("categoryView", categoryView);
        }, threadPoolExecutor);
        //查询销售属性集合
        CompletableFuture<Void> spuSaleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            resultMap.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);
        //查询map集合
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            String jsonString = JSON.toJSONString(skuValueIdsMap);
            resultMap.put("valuesSkuJson", jsonString);
        }, threadPoolExecutor);
        //查询热度数据
        CompletableFuture<Void> hotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);
        CompletableFuture.allOf(skuCompletableFuture,skuPriceCompletableFuture,
                categoryViewCompletableFuture,spuSaleAttrCompletableFuture,skuValueIdsMapCompletableFuture,hotScoreCompletableFuture).join();


//        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
//        BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
//        BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
//        List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
//        Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
//        String jsonString = JSON.toJSONString(skuValueIdsMap);
//        resultMap.put("skuInfo",skuInfo);
//        resultMap.put("price",skuPrice);
//        resultMap.put("categoryView",categoryView);
//        resultMap.put("spuSaleAttrList",spuSaleAttrList);
//        resultMap.put("valuesSkuJson",jsonString);

        return resultMap;
    }
}
