package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Administrator
 * @create 2020-04-01 9:55
 */
@Controller
public class PaymentController {
    @Autowired
    private OrderFeignClient orderFeignClient;

    @GetMapping("pay.html")
    public String success(HttpServletRequest request){
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        request.setAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }
    /**
     * 支付成功页
     * @param
     * @return
     */
    @GetMapping("pay/success.html")
    public String success() {

        return "payment/success";
    }
}
