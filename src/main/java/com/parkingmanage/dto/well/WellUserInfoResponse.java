package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔用户信息响应
 */
@Data
public class WellUserInfoResponse {
    
    /** 用户编号 */
    private String userNo;
    
    /** 用户姓名 */
    private String userName;
    
    /** 用户身份证号 */
    private String userIdentity;
    
    /** 部门名称 */
    private String deptName;
    
    /** 卡号 */
    private String cardNo;
    
    /** 性别 */
    private String sex;
    
    /** 手机号 */
    private String phone;
}
