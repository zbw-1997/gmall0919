package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author Administrator
 * @create 2020-04-01 19:34
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    @GetMapping("submit/{orderId}")
    @ResponseBody
    public String submitOrder(@PathVariable(value = "orderId") Long orderId){
        String from = "";
        try {
            from = alipayService.createaliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return from;
    }
    @RequestMapping("test")
    @ResponseBody
    public String test(){

        return "成功";
    }
    /**
     * 支付宝回调
     * @return
     */
    @RequestMapping("callback/return")
    public String callBack() {
        // 同步回调给用户展示信息
        return "redirect:" + AlipayConfig.return_order_url;
    }
    /**
     * 支付宝异步回调  必须使用内网穿透
     * @param paramMap
     * @param
     * @return
     */
    @RequestMapping("callback/notify")
    @ResponseBody
    public String alipayNotify(@RequestParam Map<String, String> paramMap) {
        System.out.println("回来了！");

        boolean signVerified = false; //调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        // 交易状态
        String trade_status = paramMap.get("trade_status");
        String out_trade_no = paramMap.get("out_trade_no");
        // true
        if (signVerified) {
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                // 但是，如果交易记录表中 PAID 或者 CLOSE  获取交易记录中的支付状态 通过outTradeNo来查询数据
                // select * from paymentInfo where out_trade_no=?
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                if (paymentInfo.getPaymentStatus() == PaymentStatus.PAID.name() || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED.name()) {
                    return "failure";
                }
                // 正常的支付成功，我们应该更新交易记录状态
                paymentService.paySuccess(out_trade_no, PaymentType.ALIPAY.name(), paramMap);
                return "success";
            }

        } else {
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }
    //退款
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable(value = "orderId")Long orderId) {
        // 调用退款接口
        boolean flag = alipayService.refund(orderId);

        return Result.ok(flag);
    }
}
