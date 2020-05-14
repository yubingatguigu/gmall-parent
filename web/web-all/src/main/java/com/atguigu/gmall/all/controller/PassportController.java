package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author gao
 * @create 2020-04-30 0:08
 */
@Controller
public class PassportController {

    @GetMapping("login.html")
    public String login(HttpServletRequest request){

        //从哪里点击。跳回哪里
        String originUrl = request.getParameter("originUrl");
        //需要保存 originUrl  前台跳转  originUrl: [[${originUrl}]],
        request.setAttribute("originUrl",originUrl);

        return "login";
    }

}
