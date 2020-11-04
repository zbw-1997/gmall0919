package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-04-06 12:55
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Override
    public List<SeckillGoods> findAll() {
        List<SeckillGoods> list = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        return list;
    }

    @Override
    public SeckillGoods getSeckillGoods(Long id) {
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(id.toString());
        return seckillGoods;
    }
    //准备下单
    @Override
    public void seckillOrder(Long skuId, String userId) {
        //获取状态位  因为商品随时可能售空
        String state = (String) CacheHelper.get(skuId.toString());
        if("0".equals(state)){
            return;
        }
        //利用redis的setnx判断用户是否下过订单
        //true是插入成功说明第一次下订单，false插入失败说明redis有订单。
        Boolean flag = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if(!flag){
            return;
        }
        //获取商品
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        //判断是否还有库存
        if(StringUtils.isEmpty(goodsId)){//说明商品已售空
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            return;
        }
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setNum(1);
        orderRecode.setUserId(userId);
        orderRecode.setOrderStr(MD5.encrypt(userId));
        orderRecode.setSeckillGoods(getSeckillGoods(skuId));

        //将订单放入缓存中（是哪个用户的订单）
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(),orderRecode);
        //更新库存
        updateStockCount(orderRecode.getSeckillGoods().getSkuId());
    }
    //根据商品id与用户ID查看订单信息
    @Override
    public Result checkOrder(Long skuId, String userId) {
        //判断用户是否在缓存中
        Boolean flag = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        if(flag){
            Boolean isHasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if(isHasKey){
                //抢单成功
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        // 判断订单
        Boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        // 如果缓存中有当前的订单了，则不能重复下单
        if (isExistOrder){

            // 获取订单
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);

            // 返回 下单成功！
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        // 判断商品的状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            // 已经售罄抢单失败！
            return Result.build(null,ResultCodeEnum.SECKILL_FAIL);
        }
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    //更新库存
    private void updateStockCount(Long skuId) {
        //获取当前库存数量
        Long size = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();
        //不需要每次都更新库存，效率太低
        if(size%2==0){
            //更新数据库
            SeckillGoods seckillGoods = new SeckillGoods();
            seckillGoods.setStockCount(size.intValue());
            seckillGoodsMapper.updateById(seckillGoods);
            //更新缓存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(),seckillGoods);
        }
    }
}
