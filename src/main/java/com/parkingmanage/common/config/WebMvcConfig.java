package com.parkingmanage.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.MultipartConfigElement;

/**
 * @PACKAGE_NAME:  com.parkingmanage.commom.config
 * @NAME: WebMvcConfig
 * @author: yuli
 * @Version: 1.0
 * @DATE: 2021/12/8 13:03
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 在配置文件中配置的文件保存路径
     */
    @Value("${file.upload-path}")
    private String uploadPath;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //文件最大KB,MB
        factory.setMaxFileSize(DataSize.parse("1024MB"));
        //设置总上传数据总大小
        factory.setMaxRequestSize(DataSize.parse("1024MB"));
        return factory.createMultipartConfig();
    }

    /**
     * 这里是映射文件路径的方法
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置模板资源路径
//        registry.addResourceHandler("/uploadfile/**").addResourceLocations(ResourceUtils.FILE_URL_PREFIX + uploadPath);
        registry.addResourceHandler("/uploadfile/**").addResourceLocations("file:C:/Users/Administrator/Desktop/static/images");
        // registry.addResourceHandler("/avatar/").addResourceLocations(ResourceUtils.FILE_URL_PREFIX+"/avatar/");
        // registry.addResourceHandler("/files/").addResourceLocations(ResourceUtils.FILE_URL_PREFIX + System.getProperty("user.dir") + "/files/");
    }
}
