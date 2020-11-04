package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author Administrator
 * @create 2020-04-01 19:31
 */
public interface AlipayService {

    String createaliPay(Long orderId) throws AlipayApiException;

    boolean refund(Long orderId);
}
