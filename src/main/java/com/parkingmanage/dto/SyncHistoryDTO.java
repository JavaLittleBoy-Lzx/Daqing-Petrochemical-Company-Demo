package com.parkingmanage.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步历史记录DTO
 * 
 * Requirements: 8.1, 8.2
 */
@Data
public class SyncHistoryDTO {
    
    /** 同步ID */
    private String syncId;
    
    /** 同步开始时间 */
    private LocalDateTime startTime;
    
    /** 同步结束时间 */
    private LocalDateTime endTime;
    
    /** 同步耗时（毫秒） */
    private long duration;
    
    /** 是否成功 */
    private boolean success;
    
    /** 错误信息 */
    private String errorMessage;
    
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
    private List<String> failedRecords;
    
    /**
     * 从SyncResult创建历史记录
     */
    public static SyncHistoryDTO fromSyncResult(SyncResult result, LocalDateTime startTime) {
        SyncHistoryDTO history = new SyncHistoryDTO();
        history.setSyncId(String.valueOf(System.currentTimeMillis()));
        history.setStartTime(startTime);
        history.setEndTime(result.getSyncTime());
        history.setDuration(result.getDuration());
        history.setSuccess(result.isSuccess());
        history.setErrorMessage(result.getErrorMessage());
        history.setPersonTotal(result.getPersonTotal());
        history.setPersonSuccess(result.getPersonSuccess());
        history.setPersonFailed(result.getPersonFailed());
        history.setVehicleTotal(result.getVehicleTotal());
        history.setVehicleSuccess(result.getVehicleSuccess());
        history.setVehicleFailed(result.getVehicleFailed());
        history.setBlacklistTotal(result.getBlacklistTotal());
        history.setBlacklistSuccess(result.getBlacklistSuccess());
        history.setFailedRecords(result.getFailedRecords());
        return history;
    }
}
