package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔门禁记录查询请求
 */
@Data
public class WellGateRecordRequest {
    
    /** 页码 */
    private Integer pageIndex;
    
    /** 每页查询条数（默认10000条，单次请求最大限制10000条） */
    private Integer pageSize;
    
    /** 查询开始时间（13位毫秒时间戳） */
    private Long beginTimestamp;
    
    /** 查询结束时间（13位毫秒时间戳） */
    private Long endTimestamp;
}
