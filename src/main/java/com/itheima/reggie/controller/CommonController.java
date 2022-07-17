package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/**
 * 文件上传/下载 通用类
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file) throws IOException {
        //file当前是临时文件.tmp，需要转存到指定位置，否则本次请求完成后临时文件就会被删除

        //原始文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.')); //取得原始后缀

        //使用UUID生成文件名，防止被覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        //转储文件
        File dir = new File(basePath);
        if (!dir.exists()){ dir.mkdir();} //判断文件夹是否存在
        file.transferTo(new File(basePath + fileName));
        return R.success(fileName);
    }

    /**
     * 文件下载
     */
    @GetMapping("/download")
    public void download(HttpServletResponse response, String name){
        try {
            response.setContentType("image/jpeg");
            //输入流，读取文件
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));
            //输出流，将文件写回浏览器，在浏览器展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            int len = 0;
            byte[] bytes = new byte[1024];
            while ((len = fileInputStream.read(bytes))!=-1){
                outputStream.write(bytes, 0, len);
                outputStream.flush();
            }

            fileInputStream.close();
            outputStream.close();

        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
