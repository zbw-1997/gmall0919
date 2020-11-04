package com.atguigu.gmall.user.controller;

import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.service.UserService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Administrator
 * @create 2020-03-25 18:20
 */
@RestController
@RequestMapping("/api/user/passport")
public class PassportController {

    @Autowired
    private UserService userService;
    @Autowired
    private RedisTemplate redisTemplate;

    @ApiOperation(value = "用户单点登陆")
    @PostMapping("login")
    public Result login(@RequestBody UserInfo userInfo){
        UserInfo info = userService.login(userInfo);
        if(info!=null){
            //用uuid定义令牌
            String token = UUID.randomUUID().toString().replaceAll("-","");
            //把token放入集合，为了让其他服务可以找到token
            Map<String,Object> map =new HashMap<>();
            map.put("token",token);
            map.put("name", info.getName());
            map.put("nickName", info.getNickName());
            //把token放入缓存中
            //定义key
            String tokenKey = RedisConst.USER_LOGIN_KEY_PREFIX+token;
            redisTemplate.opsForValue().set(tokenKey,info.getId().toString(),RedisConst.USERKEY_TIMEOUT, TimeUnit.SECONDS);

        return Result.ok(map);
        }else{
            return Result.fail().message("用户名或密码错误");
        }
    }
    @ApiOperation(value = "用户注销")
    @GetMapping("logout")
    public Result logout(HttpServletRequest request){
        //获取token(不使用cookie获取token原因是因为springboot+springcloud远程调用时cookie无法携带数据)
        //所以使用header获取token
        String token = request.getHeader("token");
        redisTemplate.delete(RedisConst.USER_LOGIN_KEY_PREFIX+token);
        return Result.ok();
    }

}
