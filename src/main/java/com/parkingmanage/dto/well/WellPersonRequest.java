package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔人员请求DTO
 * 对应接口：/api-general/api-general/open-user/batch/insert-or-update
 */
@Data
public class WellPersonRequest {
    
    /** 所属组织行政编号 */
    private String ptSourceNo;
    
    /** 用户名称 */
    private String userName;
    
    /** 用户编号(工号)-唯一约束 */
    private String sourceNo;
    
    /** 用户类型：4.学生 5.老师 6.职工 */
    private Integer userType;
    
    /** 用户状态 */
    private Integer userState;
    
    /** 性别：0-未知 1-男 2-女 */
    private Integer userSex;
    
    /** 身份证号 */
    private String userIdentity;
    
    /** 手机号 */
    private String phoneNo;
    
    /** 备注 */
    private String remark;
}
