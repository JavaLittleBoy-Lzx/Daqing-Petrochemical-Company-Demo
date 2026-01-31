package com.parkingmanage.dto.ake;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AKE接口统一响应格式
 * 
 * 响应示例：
 * {
 *     "command": "REPORT_CAR_IN_LIST",
 *     "message_id": "00000000100",
 *     "sign_type": "MD5",
 *     "sign": "f3AKCWksumTLzW5Pm38xiP9llqwHptZl9QJQxcm7zRvcXA4g",
 *     "charset": "UTF-8",
 *     "timestamp": "20141208164130",
 *     "biz_content": {
 *         "code": "0",
 *         "msg": "ok"
 *     }
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AkeResponse {
    
    /**
     * 命令类型
     */
    @JsonProperty("command")
    private String command;
    
    /**
     * 消息ID（回传请求中的message_id）
     */
    @JsonProperty("message_id")
    private String messageId;
    
    /**
     * 签名类型
     */
    @JsonProperty("sign_type")
    private String signType;
    
    /**
     * 签名
     */
    @JsonProperty("sign")
    private String sign;
    
    /**
     * 字符集
     */
    @JsonProperty("charset")
    private String charset;
    
    /**
     * 时间戳（格式：yyyyMMddHHmmss）
     */
    @JsonProperty("timestamp")
    private String timestamp;
    
    /**
     * 业务内容
     */
    @JsonProperty("biz_content")
    private BizContent bizContent;
    
    /**
     * 业务内容
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BizContent {
        /**
         * 响应码：0-成功，其他-失败
         */
        @JsonProperty("code")
        private String code;
        
        /**
         * 响应消息
         */
        @JsonProperty("msg")
        private String msg;
    }
    
    /**
     * 创建成功响应
     * 
     * @param command 命令类型（从请求中获取）
     * @param messageId 消息ID（从请求中获取）
     * @return AKE响应对象
     */
    public static AkeResponse success(String command, String messageId) {
        return AkeResponse.builder()
                .command(command)
                .messageId(messageId)
                .signType("MD5")
                .sign("") // TODO: 如果需要签名，这里需要实现签名逻辑
                .charset("UTF-8")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .bizContent(BizContent.builder()
                        .code("0")
                        .msg("ok")
                        .build())
                .build();
    }
    
    /**
     * 创建失败响应
     * 
     * @param command 命令类型（从请求中获取）
     * @param messageId 消息ID（从请求中获取）
     * @param errorMsg 错误消息
     * @return AKE响应对象
     */
    public static AkeResponse error(String command, String messageId, String errorMsg) {
        return AkeResponse.builder()
                .command(command)
                .messageId(messageId)
                .signType("MD5")
                .sign("") // TODO: 如果需要签名，这里需要实现签名逻辑
                .charset("UTF-8")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")))
                .bizContent(BizContent.builder()
                        .code("1")
                        .msg(errorMsg)
                        .build())
                .build();
    }
}
