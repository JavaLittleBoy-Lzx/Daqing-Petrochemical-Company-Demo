package com.parkingmanage.dto.ake;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AKE进出场记录请求基类
 * 包含公共字段
 */
@Data
public class AkeRecordRequest {
    
    /** 命令类型：REPORT_CAR_IN_LIST(进场) / REPORT_CAR_OUT_LIST(离场) */
    @JsonProperty("command")
    private String command;
    
    /** 消息ID */
    @JsonProperty("message_id")
    private String messageId;
    
    /** 设备ID */
    @JsonProperty("device_id")
    private String deviceId;
    
    /** 签名类型 */
    @JsonProperty("sign_type")
    private String signType;
    
    /** 签名 */
    @JsonProperty("sign")
    private String sign;
    
    /** 字符集 */
    @JsonProperty("charset")
    private String charset;
    
    /** 时间戳 */
    @JsonProperty("timestamp")
    private String timestamp;
    
    /** 业务内容 */
    @JsonProperty("biz_content")
    private Object bizContent;
}
