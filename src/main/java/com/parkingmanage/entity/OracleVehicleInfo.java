package com.parkingmanage.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Oracle车辆信息实体
 * 从Oracle数据库视图 pentranceguard.vew_autovalidinfo 获取的车辆原始数据
 */
@Data
public class OracleVehicleInfo {
    
    /** 车牌号码（CPHM） */
    private String plateNumber;
    
    /** 卡号（KH） */
    private String cardNo;
    
    /** 驾驶员姓名（JSTXM） */
    private String ownerName;
    
    /** 车主电话 */
    private String ownerPhone;
    
    /** 单位名称（DWMCNAME） */
    private String company;
    
    /** 车主部门 */
    private String department;
    
    /** 厂区代码（CQDM） */
    private String orgNo;
    
    /** 厂区名称（CQDMNAME） */
    private String orgName;
    
    /** 车辆类型名称（CLLXNAME） */
    private String vehicleType;
    
    /** 车辆类别名称（CLLBNAME） */
    private String vehicleCategory;
    
    /** 号牌颜色名称（HPTSNAME） */
    private String plateColor;
    
    /** 品牌型号（PPXH） */
    private String brandModel;
    
    /** VIP类型名称 */
    private String vipTypeName;
    
    /** 有效期开始（KYXQKSSJ） */
    private LocalDateTime validStartTime;
    
    /** 有效期结束（KYXQJSSJ） */
    private LocalDateTime validEndTime;
    
    /** 是否需要停车检查（ISCHECK，黑名单标记） */
    private boolean needCheck;
    
    /** 检查原因（ISCHECKNAME） */
    private String checkReason;
    
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 当前状态代码（DQZT）- D=注销，A=正常 */
    private String dqzt;

    /** 备注/当前状态名称（DQZTNAME） */
    private String remark;

    /** 卡类型（KLX）- A=长期卡，D=临时卡 */
    private String klx;
}
