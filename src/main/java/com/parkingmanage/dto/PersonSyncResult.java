package com.parkingmanage.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 人员同步结果DTO
 */
@Data
public class PersonSyncResult {
    
    /** 总数 */
    private int total;
    
    /** 成功数 */
    private int success;
    
    /** 失败数 */
    private int failed;
    
    /** 新增数 */
    private int inserted;
    
    /** 更新数 */
    private int updated;
    
    /** 人脸上传成功数 */
    private int faceSuccess;
    
    /** 人脸上传失败数 */
    private int faceFailed;
    
    /** 授权成功数 */
    private int grantSuccess;
    
    /** 授权失败数 */
    private int grantFailed;
    
    /** 失败记录列表 */
    private List<FailedRecord> failedRecords = new ArrayList<>();
    
    /**
     * 添加失败记录
     * @param employeeNo 员工编号
     * @param name 姓名
     * @param operation 操作类型：INSERT/UPDATE/FACE/GRANT
     * @param reason 失败原因
     */
    public void addFailedRecord(String employeeNo, String name, String operation, String reason) {
        if (failedRecords == null) {
            failedRecords = new ArrayList<>();
        }
        FailedRecord record = new FailedRecord();
        record.setEmployeeNo(employeeNo);
        record.setName(name);
        record.setOperation(operation);
        record.setReason(reason);
        failedRecords.add(record);
    }
    
    /**
     * 失败记录
     */
    @Data
    public static class FailedRecord {
        /** 员工编号 */
        private String employeeNo;
        
        /** 姓名 */
        private String name;
        
        /** 操作类型：INSERT/UPDATE/FACE/GRANT */
        private String operation;
        
        /** 失败原因 */
        private String reason;
    }
}
