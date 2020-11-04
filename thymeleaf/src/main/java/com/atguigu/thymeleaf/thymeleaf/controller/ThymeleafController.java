package com.atguigu.thymeleaf.thymeleaf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Administrator
 * @create 2020-03-17 20:47
 */
@Controller
public class ThymeleafController {

    @RequestMapping("index")
    public String index(HttpServletRequest request, HttpSession session){
        request.setAttribute("age",18);
        request.setAttribute("name","郑博文");
        session.setAttribute("username","郑博文");
        List<String> list =new ArrayList<>();
        list.add("张三");
        list.add("李四");
        list.add("王五");
        list.add("李刘");
        request.setAttribute("list",list);

        session.setAttribute("iphone","<span style=color:red>iphone11</span>");
        session.setAttribute("ask","好的");
        return "index";
    }
    @RequestMapping("list.html")
    public String list(String id,HttpSession session){
        session.setAttribute("aaa",id);
        return "list";
    }
}
