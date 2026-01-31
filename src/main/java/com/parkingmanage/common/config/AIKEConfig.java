package com.parkingmanage.common.config;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.Controller;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * @ClassName AIKEConfig
 * @Description 用于定义AIKE的常量
 * @Author lzx
 * @Date 2023/8/16 8:09:40
 * @Version 1.0
 **/
@Component
public class AIKEConfig {

    private static final Logger logger = LoggerFactory.getLogger(AIKEConfig.class);

    //    public static final String AK_URL = "https://open.yidianting.xin/openydt/api/v2/";
//      public static final String AK_URL = "https://open.yidianting.xin/openydt/api/v2/";
    public static final String AK_URL = "https://open.yidianting.xin/openydt/api/v3/";
    /**
     * 授权的secret
     */
//    public static final String AK_SECRET = "ute9hhadk7r17ng149il983w96uv6qks";
//    public static final String AK_SECRET = "k2inrrsaump11mc5lo7l5k4bksfrm2vw";
    public static final String AK_SECRET = "IE08yjeeplR2R5ST0ISzh8kOo344xl04";
    /**
     * 授权的key
     */
//    public static final String AK_KEY = "up5kzt";
//    public static final String AK_KEY = "vjtrpa";
    public static final String AK_KEY = "202404232733";

    //拼接方法
    /**
     * 一点停开放平台下行接口调用 方法1：请求为map
     *
     * @param url    下行接口地址
     * @param key    授权标识
     * @param secret 授权秘钥
     * @param cmd    下行接口签名
     * @param params 参数
     * @return 返回为字符串
     */
    public JSONObject downHandler(String url, String key, String secret, String cmd, Map<String, Object> params) {
        long startTime = System.currentTimeMillis();
        String fullUrl = url + cmd;
        
        try {
            // 生成签名和认证信息
            String timestamp = DateUtil.format(new Date(), "yyyyMMddHHmmss");
            String sign = SecureUtil.md5(key + ":" + timestamp + ":" + secret);
            sign = sign.toLowerCase();
            String auth = Base64.encode(key + ":" + timestamp);
            // 发送HTTP请求
                         String resultStr = HttpRequest.post(fullUrl + "?sign=" + sign)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json;charset=utf-8")
                .header("Authorization", auth)
                .body(JSONObject.toJSONString(params))
                .timeout(28000)  // 设置8秒超时，加快响应速度
                .execute()
                .body();
            long endTime = System.currentTimeMillis();
            logger.info("📥 外部API响应成功 - 耗时: {}ms, 响应长度: {} 字符", (endTime - startTime), resultStr != null ? resultStr.length() : 0);
            // 打印响应内容（可能很长，所以只打印前500字符）
            if (resultStr != null && resultStr.length() > 500) {
                logger.info("📄 API响应内容(前500字符)");
            } else {
                logger.info("📄 API响应内容");
            }
            JSONObject response = JSONObject.parseObject(resultStr);
            // 检查响应是否包含错误
            if (response != null && response.containsKey("code") && !response.getString("code").equals("0")) {
                logger.warn("⚠️ API返回业务错误 - code: {}, message: {}", response.getString("code"), response.getString("message"));
            }
            return response;
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            logger.error("❌ 外部API调用失败 - 接口: {}, 命令: {}, 耗时: {}ms, 错误: {}", 
                fullUrl, cmd, (endTime - startTime), e.getMessage(), e);
            // 返回一个错误响应而不是null
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("code", "-1");
            errorResponse.put("message", "API调用异常: " + e.getMessage());
            errorResponse.put("success", false);
            return errorResponse;
        }
    }
    public static String getCommonResponse(Boolean success) {
        Map<String, Object> params = new HashMap<>();
        params.put("status", 1);
        params.put("message", "ok");
        if (success) {
            params.put("resultCode", 0);
        } else {
            params.put("resultCode", -1);
            params.put("data", new JSONObject());
        }
        return JSONObject.toJSONString(params);
    }
}