package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author Administrator
 * @create 2020-04-06 12:32
 */
@Component
public class SeckillReceiver {
    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SeckillGoodsService seckillGoodsService;

    //把商品放入缓存中
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importItemToRedis(Message message, Channel channel){
        //根据条件从数据库中查询秒杀商品
        QueryWrapper<SeckillGoods> wrapper =new QueryWrapper<>();
        //状态为1，库存大于0，创建时间为今天
        wrapper.eq("status",1).gt("stock_count",0);
        wrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(wrapper);
        if(!CollectionUtils.isEmpty(seckillGoodsList)){
            //放入缓存中
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                if(flag){//说明缓存中有数据
                    continue;
                }
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);
                //根据商品数量把商品用list存储在redis中，避免超卖问题
                for (int i = 0; i < seckillGoods.getStockCount(); i++) {
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }
                //利用reids的订阅发布通知其他节点添加与更新状态位，更新为开启
                redisTemplate.convertAndSend("seckillpush", seckillGoods.getSkuId()+":1");
            }

            //手动确认消息成功
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

        }
    }
    //把秒杀用户放入到队列中
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckill(UserRecode userRecode,Message message, Channel channel) {
        if (null != userRecode) {
            //Log.info("paySuccess:"+ JSONObject.toJSONString(userRecode));
            //预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(), userRecode.getUserId());
        }
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
    /**
     * 秒杀结束清空缓存
     *
     * @param message
     * @param channel
     * @throws IOException
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18, durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK, type = ExchangeTypes.DIRECT, durable = "true"),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedis(Message message, Channel channel) throws IOException {

        //活动结束清空缓存
        QueryWrapper<SeckillGoods> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1);
        queryWrapper.le("end_time", new Date());
        List<SeckillGoods> list = seckillGoodsMapper.selectList(queryWrapper);
        //清空缓存
        for (SeckillGoods seckillGoods : list) {
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX + seckillGoods.getSkuId());
        }
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);
        //将状态更新为结束
        SeckillGoods seckillGoodsUp = new SeckillGoods();
        seckillGoodsUp.setStatus("2");
        seckillGoodsMapper.update(seckillGoodsUp, queryWrapper);
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
