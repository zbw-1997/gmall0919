package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author Administrator
 * @create 2020-03-25 18:18
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    public UserInfo login(UserInfo userInfo) {
        //根据用户名密码查询用户信息
        String loginName = userInfo.getLoginName();
        String passwd = userInfo.getPasswd();
        //用md5加密
        String newpwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        QueryWrapper<UserInfo> wrapper =new QueryWrapper<>();
        wrapper.eq("login_name",loginName);
        wrapper.eq("passwd",newpwd);
        UserInfo info = userInfoMapper.selectOne(wrapper);
        if(info!=null){
            return info;
        }
        return null;
    }
}
