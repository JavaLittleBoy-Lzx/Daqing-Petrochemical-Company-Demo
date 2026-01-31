package com.parkingmanage.dto.oracle;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 车辆有效信息DTO
 * 对应Oracle视图：aentranceguard.view_autovalidinfo
 */
@Data
public class VehicleValidInfoDTO {
    
    /** 记录号L */
    private String recordnoL;
    
    /** 记录号 */
    private String recordno;
    
    /** 卡号 */
    private String cardNo;
    
    /** 车牌号码 */
    private String plateNumber;
    
    /** 号牌颜色代码 */
    private String plateColorCode;
    
    /** 号牌颜色名称 */
    private String plateColorName;
    
    /** 车辆种类代码 */
    private String vehicleCategoryCode;
    
    /** 车辆种类名称 */
    private String vehicleCategoryName;
    
    /** 车辆类型代码 */
    private String vehicleTypeCode;
    
    /** 车辆类型名称 */
    private String vehicleTypeName;
    
    /** 品牌型号 */
    private String brandModel;
    
    /** 单位名称 */
    private String companyName;
    
    /** 驾驶员姓名 */
    private String driverName;
    
    /** 代码 */
    private String code;
    
    /** 厂区代码 */
    private String areaCode;
    
    /** 厂区名称 */
    private String areaName;
    
    /** 有效期开始时间 */
    private LocalDateTime validStartTime;
    
    /** 有效期结束时间 */
    private LocalDateTime validEndTime;
    
    /** 当前状态代码 */
    private String statusCode;
    
    /** 当前状态名称 */
    private String statusName;
    
    /** 是否检查 */
    private String isCheck;
    
    /** 检查名称 */
    private String checkName;
    
    /** 操作时间 */
    private String operateTime;
}
