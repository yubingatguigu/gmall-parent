package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author gao
 * @create 2020-04-29 18:03
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;

    //http://list.gmall.com/list.html?category3Id=61  ?后面的传递的参数与接受对象的属性名称一致会自动映射
   //@RequestMapping() 能够接收get，post请求
    @GetMapping("list.html")
    public String list(SearchParam searchParam, Model model){

        //将数据保存，在index.html页面渲染
       Result<Map> result = listFeignClient.list(searchParam);
       model.addAllAttributes(result.getData());
       //页面渲染时需要urlParam，记录拼接url后面的参数
       String urlParam = makeUrlParam(searchParam);//接收用户的查询条件
       //获取品牌传递的参数
       String trademark = getTrademark(searchParam.getTrademark());
       //获取平台属性
        List<Map<String, String>> propsList = getMakeProps(searchParam.getProps());
        //获取排序规则
        Map<String, Object> map = getOrder(searchParam.getOrder());

        //保存用户查询数据
       model.addAttribute("searchParam",searchParam);
       model.addAttribute("urlParam",urlParam);
       model.addAttribute("trademarkParam",trademark);
       model.addAttribute("propsParamList",propsList);
       model.addAttribute("orderMap",map);

       return "list/index";
   }

   //记录查询条件 url后面的拼接
    private String makeUrlParam(SearchParam searchParam) {
        StringBuilder urlParam = new StringBuilder();
        //判断关键字 http://list.gmall.com/list.html?keyword=手机
        if(searchParam.getKeyword()!=null){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        //判断一级分类
        if(searchParam.getCategory1Id()!=null){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        //判断二级分类
        if(searchParam.getCategory2Id()!=null){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        //判断二级分类    http://list.gmall.com/list.html?category3Id=61
        if(searchParam.getCategory3Id()!=null){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        //判断品牌
        if(searchParam.getTrademark()!=null){
            if(searchParam.getTrademark().length()>0){
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        //判断平台属性值
        if(searchParam.getProps()!=null){
            for (String prop : searchParam.getProps()) {
                if(urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        //记录拼接条件
        String urlParamStr = urlParam.toString();
        return "list.html?"+urlParamStr;
    }

    //获取品牌名称    trademark=品牌：华为
    private String getTrademark(String trademark){
        if(trademark!=null&&trademark.length()>0){
            //将字符串进行分割  trademark=3：华为
            String[] split = StringUtils.split(trademark, ":");
            //判断是否符合数据格式
            if(split!=null&&split.length==2){
                return "品牌:"+split[1];
            }
        }
        return "";
    }

    //获取平台属性值过滤得到面包屑    &trans=1&JL=3_机身存储_128GB#J_crumbsBar
    private List<Map<String,String>> getMakeProps(String[] props){
        ArrayList<Map<String, String>> list = new ArrayList<>();
        if(null!=props&&props.length>0){
            for (String prop : props) {
                //进行分割
                String[] split = StringUtils.split(prop,":");
                if(null!=split&&split.length==3){
                    //将字符串的数据放到map中，保存平台属性id  平台属性值  平台属性名称
                    HashMap<String, String> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    //获取排序规则
    private Map<String,Object> getOrder(String order){
        HashMap<String, Object> map = new HashMap<>();
        if(StringUtils.isNotEmpty(order)) {
            //进行分割
            String[] split = order.split(":");
            //判断是否符合格式
            if(split!=null&&split.length==2){
                map.put("type",split[0]);//字段
                map.put("sort",split[1]);//排序规则
            }
        }else {
            //没有排序规则
            map.put("type","1");
            map.put("sort","asc");
        }
        return map;
    }
}
