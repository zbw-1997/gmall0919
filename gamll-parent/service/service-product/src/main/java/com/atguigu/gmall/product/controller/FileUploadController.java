package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.FilenameUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author Administrator
 * @create 2020-03-15 10:28
 */
@RestController
@RequestMapping("admin/product")
public class FileUploadController {
    @Value("${fileServer.url}")
    private String fileUrl;

    @ApiOperation("文件上传")
    @RequestMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws IOException, MyException {
        String path = null;
        if (file != null) {
            //获取recourse目录下的conf文件
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            //初始化配置
            ClientGlobal.init(configFile);
            //创建trackerClient对象
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServer对象
            TrackerServer trackerServer = trackerClient.getConnection();
            //创建storageClient1对象
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, null);
            //上传文件返回path
            path = storageClient1.upload_appender_file1(file.getBytes(), FilenameUtils.getExtension(file.getOriginalFilename()), null);

        }
        return Result.ok(fileUrl + path);
    }
}
