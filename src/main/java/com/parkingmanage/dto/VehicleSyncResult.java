package com.parkingmanage.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 车辆同步结果DTO
 */
@Data
public class VehicleSyncResult {
    
    /** 总数 */
    private int total;
    
    /** 成功数 */
    private int success;
    
    /** 失败数 */
    private int failed;
    
    /** VIP开通成功数 */
    private int vipOpenSuccess;
    
    /** VIP开通失败数 */
    private int vipOpenFailed;
    
    /** VIP退票成功数 */
    private int vipRefundSuccess;
    
    /** VIP退票失败数 */
    private int vipRefundFailed;
    
    /** VIP续费成功数 */
    private int vipRenewSuccess;
    
    /** VIP续费失败数 */
    private int vipRenewFailed;
    
    /** 黑名单添加成功数 */
    private int blacklistSuccess;
    
    /** 黑名单添加失败数 */
    private int blacklistFailed;
    
    /** 失败记录列表 */
    private List<FailedRecord> failedRecords = new ArrayList<>();
    
    /**
     * 添加失败记录
     */
    public void addFailedRecord(String plateNumber, String ownerName, String operation, String reason) {
        if (failedRecords == null) {
            failedRecords = new ArrayList<>();
        }
        FailedRecord record = new FailedRecord();
        record.setPlateNumber(plateNumber);
        record.setOwnerName(ownerName);
        record.setOperation(operation);
        record.setReason(reason);
        failedRecords.add(record);
    }
    
    /**
     * 失败记录
     */
    @Data
    public static class FailedRecord {
        /** 车牌号 */
        private String plateNumber;
        
        /** 车主姓名 */
        private String ownerName;
        
        /** 操作类型：VIP_OPEN/VIP_REFUND/BLACKLIST */
        private String operation;
        
        /** 失败原因 */
        private String reason;
    }
}
