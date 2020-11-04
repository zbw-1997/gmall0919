package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-27 12:45
 */
public interface CartService {

    void addToCart(Long skuId, String userId, Integer skuNum);

    //查询购物车列表
    //分为登录和未登录的情况
    List<CartInfo> getCartList(String userId,String userTempId);
    //更新选中状态
    void checkCart(String userId, Integer isChecked, Long skuId);
    //删除购物车
    void deleteCart(String userId,Long skuId);
    //根据用户Id 查询购物车列表
    List<CartInfo> getCartCheckedList(String userId);

    List<CartInfo> loadCartCache(String userId);
}
