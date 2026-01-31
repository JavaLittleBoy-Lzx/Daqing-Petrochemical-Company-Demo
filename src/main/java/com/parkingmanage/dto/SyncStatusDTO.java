package com.parkingmanage.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 同步状态DTO
 * 用于查询同步状态接口返回
 * 
 * Requirements: 8.1, 8.2
 */
@Data
public class SyncStatusDTO {
    
    /** 是否正在运行 */
    private boolean running;
    
    /** 上次同步时间 */
    private LocalDateTime lastSyncTime;
    
    /** 当前时间 */
    private LocalDateTime currentTime;
    
    /** 最近同步结果 */
    private SyncResult lastSyncResult;
    
    /** 最近同步历史记录 */
    private List<SyncHistoryDTO> recentHistory;
    
    /** 失败记录统计 */
    private FailedRecordSummary failedSummary;
    
    /**
     * 失败记录统计
     */
    @Data
    public static class FailedRecordSummary {
        /** 人员失败总数 */
        private int personFailedCount;
        
        /** 车辆失败总数 */
        private int vehicleFailedCount;
        
        /** 最近失败记录 */
        private List<String> recentFailedRecords;
    }
}
