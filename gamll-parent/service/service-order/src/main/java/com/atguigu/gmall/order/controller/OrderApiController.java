package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Administrator
 * @create 2020-03-29 12:40
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private CartFeignClient cartFeignClient;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductFeignClient productFeignClient;

    @ApiOperation(value = "订单列表")
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        //点击去结算时，需要用户的id
        String userId = AuthContextHolder.getUserId(request);
        //地址详情
        List<UserAddress> userAddressListByUserId = userFeignClient.findUserAddressListByUserId(userId);
        //送货详情（订单详情）
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        //声明一个订单详情集合来存储购物车中的被选中的商品
        List<OrderDetail> orderDetailList =new ArrayList<>();
        for (CartInfo cartInfo : cartCheckedList) {
            OrderDetail orderDetail =new OrderDetail();
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            //获取实时价格
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetailList.add(orderDetail);
        }
        //计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();
        //把数据封装到map集合中
        Map<String,Object> map =new HashMap<>();
        map.put("userAddressList",userAddressListByUserId);
        map.put("detailArrayList",orderDetailList);
        map.put("totalNum",orderDetailList.size());
        map.put("totalAmount",orderInfo.getTotalAmount());
        // 获取流水号
        String tradeNo = orderService.tradeNo(userId);
        map.put("tradeNo", tradeNo);
        return Result.ok(map);
    }


    @ApiOperation(value = "提交订单")
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo, HttpServletRequest request) {
        // 获取到用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));

        // 获取前台页面的流水号
        String tradeNo = request.getParameter("tradeNo");

        // 调用服务层的比较方法
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        if (!flag) {
            // 比较失败！
            return Result.fail().message("不能重复提交订单！");
        }

        //  删除流水号
        orderService.deleteTradeNo(userId);
        // 验证库存：
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 验证库存：
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                return Result.fail().message(orderDetail.getSkuName() + "库存不足！");
            }
            // 验证价格：
            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            if (orderDetail.getOrderPrice().compareTo(skuPrice) != 0) {
                // 重新查询价格！
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message(orderDetail.getSkuName() + "价格有变动！");
            }
        }
        // 验证通过，保存订单！
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }
    @ApiOperation(value = "根据订单id 获取订单信息")
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo(@PathVariable Long orderId){

        return orderService.getOrderInfo(orderId);
    }
    /**
     * 秒杀提交订单，秒杀订单不需要做前置判断，直接下单
     * @param orderInfo
     * @return
     */
    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo) {
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }
}
