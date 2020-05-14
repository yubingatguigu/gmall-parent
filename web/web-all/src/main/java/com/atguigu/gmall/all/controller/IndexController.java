package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileWriter;
import java.io.FilterWriter;
import java.io.IOException;

/**
 * @author gao
 * @create 2020-04-27 0:02
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    //模板引擎
    @Autowired
    private SpringTemplateEngine springTemplateEngine;

    //自己创建静态的页面
    @GetMapping("/createHtml")
    @ResponseBody
    public Result createHtml() throws IOException {
        //三级分类数据
        Result result = productFeignClient.getBaseCategoryList();
        Context context = new Context();
        context.setVariable("list",result.getData());//分类数据
        //在D盘生成一个index，html文件
        FileWriter fileWriter = new FileWriter("D://index.html");
        //使用模板引擎
        springTemplateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();
    }

    //首页
//    @GetMapping({"/","index.html"})
//    public String index(){
//        return "index";
//    }

    //从缓存中直接获取数据渲染
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());
        return "index/index";
    }

}
