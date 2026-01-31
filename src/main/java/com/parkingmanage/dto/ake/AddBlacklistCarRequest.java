package com.parkingmanage.dto.ake;

import lombok.Data;

/**
 * AKE添加黑名单请求DTO
 * 对应接口：ADD_BLACK_LIST_CAR (4.17)
 */
@Data
public class AddBlacklistCarRequest {
    
    /** 黑名单类型编码 */
    private String vipTypeCode;
    
    /** 黑名单类型名称 */
    private String vipTypeName;
    
    /** 车牌号 */
    private String carCode;
    
    /** 车主 */
    private String carOwner;
    
    /** 原因 */
    private String reason;
    
    /** 是否永久 0-临时 1-永久 */
    private Integer isPermament;
    
    /** 时间段（临时时必填） */
    private TimePeriod timePeriod;
    
    /** 备注1 */
    private String remark1;
    
    /** 备注2 */
    private String remark2;
    
    /** 操作人 */
    private String operator;
    
    /** 操作时间 */
    private String operateTime;
    
    @Data
    public static class TimePeriod {
        /** 开始时间 */
        private String startTime;
        
        /** 结束时间 */
        private String endTime;
    }
}
