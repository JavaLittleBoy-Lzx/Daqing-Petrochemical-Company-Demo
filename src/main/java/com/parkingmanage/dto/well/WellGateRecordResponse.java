package com.parkingmanage.dto.well;

import lombok.Data;

/**
 * 威尔门禁记录响应
 */
@Data
public class WellGateRecordResponse {
    
    /** 流水号 */
    private String flowNo;
    
    /** 场所名称 */
    private String placeName;
    
    /** 对应门号（1~4） */
    private String doorNo;
    
    /** 门名称 */
    private String doorName;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 工号 */
    private String userNo;
    
    /** 用户姓名 */
    private String userName;
    
    /** 部门名称 */
    private String deptName;
    
    /** 卡号 */
    private String cardNo;
    
    /** 记录照片 */
    private String recPhoto;
    
    /** 识别方式 */
    private String authMode;
    
    /** 门方向（0-进门 1-出门 255-无） */
    private String recDic;
    
    /** 出入记录状态（0-无效 1-有效 2-报警） */
    private String recStatus;
    
    /** 记录类型 */
    private String recType;
    
    /** 记录时间 (yyyy-MM-dd HH:mm:ss) */
    private String recTime;
    
    /** 来源编号 */
    private String sourceNo;
}
