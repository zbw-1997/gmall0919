package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author Administrator
 * @create 2020-03-29 14:09
 */
public interface OrderService extends IService<OrderInfo> {

    //保存订单
    Long saveOrderInfo(OrderInfo orderInfo);
    //生成流水号
    String tradeNo(String userId);
    //比较流水号
    boolean checkTradeCode(String userId ,String tradeNo);
    //删除流水号
    void deleteTradeNo(String userId);
    //验证库存
    boolean checkStock(Long skuId, Integer skuNum);
    //处理过期订单
    void execExpiredOrder(Long orderId);
    //根据订单Id 修改订单的状态
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);
    //根据订单id 获取订单信息
    OrderInfo getOrderInfo(Long orderId);
}
