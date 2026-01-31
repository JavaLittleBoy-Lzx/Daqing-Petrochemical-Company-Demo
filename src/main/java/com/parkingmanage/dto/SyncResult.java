package com.parkingmanage.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 同步结果DTO
 */
@Data
public class SyncResult {
    
    /** 同步时间 */
    private LocalDateTime syncTime;
    
    /** 人员总数 */
    private int personTotal;
    
    /** 人员成功数 */
    private int personSuccess;
    
    /** 人员失败数 */
    private int personFailed;
    
    /** 车辆总数 */
    private int vehicleTotal;
    
    /** 车辆成功数 */
    private int vehicleSuccess;
    
    /** 车辆失败数 */
    private int vehicleFailed;
    
    /** 黑名单总数 */
    private int blacklistTotal;
    
    /** 黑名单成功数 */
    private int blacklistSuccess;
    
    /** 失败记录 */
    private List<String> failedRecords = new ArrayList<>();
    
    /** 同步耗时（毫秒） */
    private long duration;
    
    /** 是否成功 */
    private boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
    /**
     * 添加失败记录
     */
    public void addFailedRecord(String record) {
        if (failedRecords == null) {
            failedRecords = new ArrayList<>();
        }
        failedRecords.add(record);
    }
}
