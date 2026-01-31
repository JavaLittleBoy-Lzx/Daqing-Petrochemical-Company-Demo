package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔人-门授权请求DTO
 * 对应接口：/api-gating/api-gating/open-gating-grant/batch/insert-or-update
 */
@Data
public class WellGrantRequest {
    
    /** 人员id（没有时传userNo） */
    private Integer userId;
    
    /** 人员工号 */
    private String userNo;
    
    /** 门id */
    private Integer doorId;
    
    /** 进门规则id */
    private Integer ruleId;
    
    /** 出门规则id */
    private Integer outRuleId;
    
    /** 生效方式 1:正向(默认) 2:反向 */
    private Integer effectWay;
    
    /** 三方授权编号 */
    private String sourceNo;
}
