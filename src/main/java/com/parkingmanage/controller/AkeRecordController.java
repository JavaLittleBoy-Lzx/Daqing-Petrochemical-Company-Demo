package com.parkingmanage.controller;

import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.service.ake.AkeRecordService;
import com.parkingmanage.service.oracle.OracleRecordWriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AKE进出场记录接收控制器
 * 接收AKE系统推送的进出场记录
 * 
 * 使用Map接收数据，以应对接口参数可能变化的情况
 * 响应格式遵循AKE接口规范
 */
@Slf4j
@RestController
@RequestMapping("/api/ake/record")
@Api(tags = "AKE进出场记录接收")
public class AkeRecordController {

    @Autowired
    private AkeRecordService akeRecordService;
    
    @Autowired
    private OracleRecordWriteService oracleRecordWriteService;

    /**
     * 接收进场记录
     * 
     * @param body 原始进场记录JSON数据（String格式）
     * @return AKE标准响应格式
     */
    @PostMapping(value = "/reportCarIn", consumes = MediaType.ALL_VALUE)
    @ApiOperation("接收进场记录")
    public ResponseEntity<JSONObject> receiveCarIn(@RequestBody String body) {
        
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(body);
            // 对JSON中的URL编码字符串进行解码
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                if (bizContent != null) {
                    // 解码所有可能包含中文的字段
                    decodeUrlEncodedFields(bizContent);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 无法解析请求体为JSON，body={}", body);
        }
        
        // 获取请求中的command和message_id，用于响应
        String command = data != null ? data.getString("command") : "REPORT_CAR_IN_LIST";
        String messageId = data != null ? data.getString("message_id") : "vems";
        String deviceId = data != null ? data.getString("device_id") : "0000000000000000000000000000vems";
        
        try {
            // 获取车牌号（用于日志）
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                String carLicenseNumber = bizContent != null ? bizContent.getString("car_license_number") : "未知";
                
                log.info("消息ID: {}, 设备ID: {}, 车牌号: {}", messageId, deviceId, carLicenseNumber);
                
                // 过滤未识别的车牌号码
                if (carLicenseNumber != null && !carLicenseNumber.equals("未识别") && !carLicenseNumber.isEmpty()) {
                    // 处理进场记录
                    akeRecordService.handleCarInRecord(data);
                    log.info("✅ 进场记录处理成功: 车牌={}", carLicenseNumber);
                    
                    // 写入Oracle数据库
                    oracleRecordWriteService.writeVehicleInRecord(data);
                } else {
                    log.info("⚠️ 跳过未识别车牌号码的进场数据: {}", carLicenseNumber);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 进场记录处理失败: {}", e.getMessage(), e);
        }
        
        // 构建响应JSON
        return buildSuccessResponse(command, messageId, deviceId);
    }

    /**
     * 接收离场记录
     * 
     * @param body 原始离场记录JSON数据（String格式）
     * @return AKE标准响应格式
     */
    @PostMapping(value = "/reportCarOut", consumes = MediaType.ALL_VALUE)
    @ApiOperation("接收离场记录")
    public ResponseEntity<JSONObject> receiveCarOut(@RequestBody String body) {
        
        JSONObject data = null;
        try {
            data = JSONObject.parseObject(body);
            // 对JSON中的URL编码字符串进行解码
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                if (bizContent != null) {
                    // 解码所有可能包含中文的字段
                    decodeUrlEncodedFields(bizContent);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 无法解析请求体为JSON，body={}", body);
        }
        
        // 获取请求中的command和message_id，用于响应
        String command = data != null ? data.getString("command") : "REPORT_CAR_OUT_LIST";
        String messageId = data != null ? data.getString("message_id") : "vems";
        String deviceId = data != null ? data.getString("device_id") : "0000000000000000000000000000vems";
        
        try {
            // 获取车牌号（用于日志）
            if (data != null && data.containsKey("biz_content")) {
                JSONObject bizContent = data.getJSONObject("biz_content");
                String carLicenseNumber = bizContent != null ? bizContent.getString("car_license_number") : "未知";
                
                log.info("消息ID: {}, 设备ID: {}, 车牌号: {}", messageId, deviceId, carLicenseNumber);
                
                // 过滤未识别的车牌号码
                if (carLicenseNumber != null && !carLicenseNumber.equals("未识别") && !carLicenseNumber.isEmpty()) {
                    // 处理离场记录
                    akeRecordService.handleCarOutRecord(data);
                    log.info("✅ 离场记录处理成功: 车牌={}", carLicenseNumber);
                    
                    // 写入Oracle数据库
                    oracleRecordWriteService.writeVehicleOutRecord(data);
                } else {
                    log.info("⚠️ 跳过未识别车牌号码的离场数据: {}", carLicenseNumber);
                }
            }
            
        } catch (Exception e) {
            log.error("❌ 离场记录处理失败: {}", e.getMessage(), e);
        }
        
        // 构建响应JSON
        return buildSuccessResponse(command, messageId, deviceId);
    }
    
    /**
     * 解码URL编码的字段
     * 
     * @param bizContent 业务内容JSON对象
     */
    private void decodeUrlEncodedFields(JSONObject bizContent) {
        // 递归处理所有字段和嵌套结构
        decodeNestedObjects(bizContent);
    }
    
    /**
     * 递归解码嵌套对象中的URL编码字段
     * 
     * @param obj 待处理的对象
     */
    private void decodeNestedObjects(Object obj) {
        if (obj == null) {
            return;
        }
        
        if (obj instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) obj;
            for (String key : jsonObj.keySet()) {
                Object value = jsonObj.get(key);
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (isUrlEncoded(stringValue)) {
                        String decodedValue = safeUrlDecode(stringValue);
                        jsonObj.put(key, decodedValue);
                    }
                } else if (value instanceof JSONObject || value instanceof com.alibaba.fastjson.JSONArray) {
                    decodeNestedObjects(value);
                }
            }
        } else if (obj instanceof com.alibaba.fastjson.JSONArray) {
            com.alibaba.fastjson.JSONArray jsonArray = (com.alibaba.fastjson.JSONArray) obj;
            for (int i = 0; i < jsonArray.size(); i++) {
                Object item = jsonArray.get(i);
                if (item instanceof JSONObject || item instanceof com.alibaba.fastjson.JSONArray) {
                    decodeNestedObjects(item);
                }
            }
        }
    }
    
    /**
     * 检测字符串是否包含URL编码
     * 
     * @param str 待检测的字符串
     * @return 如果包含URL编码返回true
     */
    private boolean isUrlEncoded(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.contains("%") && (str.matches(".*%[0-9A-Fa-f]{2}.*") || 
                                     str.matches(".*%u[0-9A-Fa-f]{4}.*") || 
                                     str.matches(".*%[0-9A-Fa-f]{2}%[0-9A-Fa-f]{2}.*"));
    }
    
    /**
     * 安全解码URL编码字符串
     * 
     * @param encodedStr 编码的字符串
     * @return 解码后的字符串，如果解码失败返回原字符串
     */
    private String safeUrlDecode(String encodedStr) {
        if (encodedStr == null || encodedStr.isEmpty()) {
            return encodedStr;
        }
        
        try {
            String decoded = URLDecoder.decode(encodedStr, StandardCharsets.UTF_8.toString());
            
            // 如果解码后仍然包含编码字符，尝试多次解码
            int maxAttempts = 3;
            int attempts = 0;
            while (isUrlEncoded(decoded) && attempts < maxAttempts) {
                decoded = URLDecoder.decode(decoded, StandardCharsets.UTF_8.toString());
                attempts++;
            }
            
            return decoded;
        } catch (Exception e) {
            log.warn("URL解码失败，返回原字符串: {}", e.getMessage());
            return encodedStr;
        }
    }
    
    /**
     * 构建成功响应
     * 
     * @param command 命令
     * @param messageId 消息ID
     * @param deviceId 设备ID
     * @return 响应对象
     */
    private ResponseEntity<JSONObject> buildSuccessResponse(String command, String messageId, String deviceId) {
        JSONObject response = new JSONObject(true);
        response.put("command", command);
        response.put("message_id", messageId);
        response.put("device_id", deviceId);
        response.put("sign_type", "MD5");
        response.put("sign", "f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g");
        response.put("charset", "UTF-8");
        response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        
        JSONObject bizContent = new JSONObject(true);
        bizContent.put("code", "0");
        bizContent.put("msg", "ok");
        response.put("biz_content", bizContent);
        
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
