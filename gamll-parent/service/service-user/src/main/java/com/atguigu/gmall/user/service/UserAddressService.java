package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-28 22:28
 */

public interface UserAddressService extends IService<UserAddress> {

    //根据用户id查询用户地址
    List<UserAddress> findUserAddressListByUserId(String userId);

}
