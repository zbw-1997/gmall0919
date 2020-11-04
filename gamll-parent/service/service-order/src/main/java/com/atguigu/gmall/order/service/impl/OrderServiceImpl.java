package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.HttpClientUtil;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * @author Administrator
 * @create 2020-03-29 14:10
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RabbitService rabbitService;
    @Autowired
    private RedisTemplate redisTemplate;
    @Value("${ware.url}")
    private String WARE_URL;
    //保存订单
    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfo orderInfo) {
        //orderInfo表中没有的参数：总金额，订单状态，订单交易编号，订单描述，创建时间，过期时间，订单主题，进度状态，
        //物流单编号，夫订单编号，图片路径
        //总金额
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID.name());
        //订单交易编号，时间戳或者uuid
        //String outTradeNo = UUID.randomUUID().toString().replace("-", "");
        String outTradeNo ="Atguigu"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //订单描述
        //orderInfo.setOrderComment();
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间
        Calendar calendar =Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        //订单主题
        List<OrderDetail> orderDetailList1 = orderInfo.getOrderDetailList();
        StringBuffer tradeBody = new StringBuffer();
        for (OrderDetail orderDetail : orderDetailList1) {
            tradeBody.append(orderDetail.getSkuName()+"");
        }
        if(tradeBody.toString().length()>100){
            orderInfo.setTradeBody(tradeBody.substring(0,100));
        }else{
            orderInfo.setTradeBody(tradeBody.toString());
        }
        //设置进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID.name());
        //保存orderInfo
        orderInfoMapper.insert(orderInfo);
        //orderDetail中没有的参数：order_id和sku_id
        //保存orderDetail
        for (OrderDetail orderDetail : orderDetailList1) {
            orderDetail.setId(null);
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insert(orderDetail);
        }
        rabbitService.sendDelayMessage(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL, MqConst.ROUTING_ORDER_CANCEL, orderInfo.getId(), MqConst.DELAY_TIME);
        return orderInfo.getId();
    }
    //生成流水号
    @Override
    public String tradeNo(String userId) {
        String tradeNo = UUID.randomUUID().toString().replace("-","");
        //放入缓存中
        //定义key
        String key ="user:"+userId+":tradeCode";
        redisTemplate.opsForValue().set(key,tradeNo);
        return tradeNo;
    }
    //比较流水号
    @Override
    public boolean checkTradeCode(String userId, String tradeNo) {
        //定义key
        String key ="user:"+userId+":tradeCode";
        //从缓存中获取tradeNo
        String tradeCacheNo = (String) redisTemplate.opsForValue().get(key);

        return tradeCacheNo.equals(tradeNo);
    }
    //删除流水号
    @Override
    public void deleteTradeNo(String userId) {
        //定义key
        String key ="user:"+userId+":tradeCode";
        redisTemplate.delete(key);
    }
    //验证仓库库存
    @Override
    public boolean checkStock(Long skuId, Integer skuNum) {
        // 远程调用http://localhost:9001/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet(WARE_URL + "/hasStock?skuId=" + skuId + "&num=" + skuNum);
        return "1".equals(result);
    }
    //处理过期订单
    @Override
    public void execExpiredOrder(Long orderId) {

        updateOrderStatus(orderId, ProcessStatus.CLOSED);
    }
    //根据订单Id 修改订单的状态
    @Override
    public void updateOrderStatus(Long orderId, ProcessStatus processStatus) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus.name());
        orderInfo.setOrderStatus(processStatus.getOrderStatus().name());
        orderInfoMapper.updateById(orderInfo);
    }
    //根据订单id 获取订单信息
    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        //查询订单信息表
        OrderInfo orderInfo = orderInfoMapper.selectById(orderId);
        //查询订单详情表

        List<OrderDetail> orderDetails = orderDetailMapper.selectList(new QueryWrapper<OrderDetail>().eq("order_id", orderId));
        orderInfo.setOrderDetailList(orderDetails);
        return orderInfo;
    }
}
