package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-04-06 12:54
 */
public interface SeckillGoodsService {

    /**
     * 返回全部列表
     * @return
     */
    List<SeckillGoods> findAll();


    /**
     * 根据ID获取实体
     * @param id
     * @return
     */
    SeckillGoods getSeckillGoods(Long id);

    /**
     * 准备下单
     * @param skuId
     * @param userId
     */
    void seckillOrder(Long skuId, String userId);


    /***
     * 根据商品id与用户ID查看订单信息
     * @param skuId
     * @param userid
     * @return
     */
    Result checkOrder(Long skuId, String userId);
}
