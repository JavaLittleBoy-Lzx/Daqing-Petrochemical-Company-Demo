package com.parkingmanage.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP客户端工具类
 * 用于调用外部API
 * @author AI Assistant
 */
@Slf4j
@Component
public class HttpClientUtils {

    private static final int CONNECT_TIMEOUT = 10000; // 连接超时 10秒
    private static final int READ_TIMEOUT = 30000; // 读取超时 30秒

    /**
     * 发送POST请求
     * @param urlString 目标URL
     * @param params 参数Map
     * @return 响应内容
     * @throws Exception 请求异常
     */
    public static String post(String urlString, Map<String, String> params) throws Exception {
        HttpURLConnection connection = null;
        OutputStream outputStream = null;
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            // 创建URL连接
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "ParkingManage/1.0");

            // 构建参数字符串
            String paramString = buildParamString(params);
            log.debug("POST参数: {}", paramString);

            // 发送参数
            if (paramString.length() > 0) {
                outputStream = connection.getOutputStream();
                outputStream.write(paramString.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            }

            // 获取响应码
            int responseCode = connection.getResponseCode();
            log.debug("HTTP响应码: {}", responseCode);

            // 读取响应
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            String responseBody = response.toString();
            log.debug("HTTP响应: {}", responseBody);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP请求失败，响应码: " + responseCode + ", 响应内容: " + responseBody);
            }

            return responseBody;

        } catch (Exception e) {
            log.error("HTTP POST请求失败: {}", urlString, e);
            throw e;
        } finally {
            // 关闭资源
            closeQuietly(reader);
            closeQuietly(inputStream);
            closeQuietly(outputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 发送GET请求
     * @param urlString 目标URL
     * @param params 参数Map
     * @return 响应内容
     * @throws Exception 请求异常
     */
    public static String get(String urlString, Map<String, String> params) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        BufferedReader reader = null;

        try {
            // 构建带参数的URL
            String fullUrl = urlString;
            if (params != null && !params.isEmpty()) {
                String paramString = buildParamString(params);
                fullUrl += (urlString.contains("?") ? "&" : "?") + paramString;
            }

            log.debug("GET请求URL: {}", fullUrl);

            // 创建URL连接
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            
            // 设置请求属性
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("User-Agent", "ParkingManage/1.0");

            // 获取响应码
            int responseCode = connection.getResponseCode();
            log.debug("HTTP响应码: {}", responseCode);

            // 读取响应
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
            } else {
                inputStream = connection.getErrorStream();
            }

            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            String responseBody = response.toString();
            log.debug("HTTP响应: {}", responseBody);

            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("HTTP请求失败，响应码: " + responseCode + ", 响应内容: " + responseBody);
            }

            return responseBody;

        } catch (Exception e) {
            log.error("HTTP GET请求失败: {}", urlString, e);
            throw e;
        } finally {
            // 关闭资源
            closeQuietly(reader);
            closeQuietly(inputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 构建参数字符串
     */
    private static String buildParamString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder paramBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (paramBuilder.length() > 0) {
                paramBuilder.append("&");
            }
            try {
                paramBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()))
                           .append("=")
                           .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            } catch (UnsupportedEncodingException e) {
                log.warn("URL编码失败: {} = {}", entry.getKey(), entry.getValue());
            }
        }
        return paramBuilder.toString();
    }

    /**
     * 安静地关闭资源
     */
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                log.warn("关闭资源失败", e);
            }
        }
    }
} 