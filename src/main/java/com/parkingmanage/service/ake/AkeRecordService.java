package com.parkingmanage.service.ake;

import java.util.Map;

/**
 * AKE进出场记录服务接口
 */
public interface AkeRecordService {
    
    /**
     * 处理进场记录
     * 
     * @param data 原始进场记录数据（Map格式）
     */
    void handleCarInRecord(Map<String, Object> data);
    
    /**
     * 处理离场记录
     * 
     * @param data 原始离场记录数据（Map格式）
     */
    void handleCarOutRecord(Map<String, Object> data);
}
