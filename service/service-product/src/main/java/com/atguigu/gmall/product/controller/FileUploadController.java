package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient1;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author gao
 * @create 2020-04-20 0:30
 */
@RestController
@RequestMapping("/admin/product/")
public class FileUploadController {

    //获取配置文件服务的ip地址
    @Value("${fileServer.url}")
    private String fileUrl;

    //文件上传返回的的地址
    @RequestMapping("fileUpload")
    public Result fileUpload(MultipartFile file) throws IOException, MyException {

        String configFile = this.getClass().getResource("/tracker.conf").getFile();
        String path = null;
        if(configFile!=null){
            //初始化
            ClientGlobal.init(configFile);
            // 创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            // 获取trackerService
            TrackerServer trackerServer  = trackerClient.getConnection();
            // 创建storageClient1
            StorageClient1 storageClient1 = new StorageClient1(trackerServer,null);
            //上传文件  第一个参数 上传文件的字节数组，文件的后缀名，数组
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);
            System.out.println("图片的路径："+fileUrl+path);
        }
        return Result.ok(fileUrl+path);
    }

}
