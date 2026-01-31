package com.parkingmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 超时车辆信息VO
 * 
 * @author System
 */
@Data
@ApiModel(value = "TimeoutVehicleVO", description = "超时车辆信息")
public class TimeoutVehicleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "预约ID")
    private Integer appointmentId;

    @ApiModelProperty(value = "车牌号")
    private String plateNumber;

    @ApiModelProperty(value = "停车场名称")
    private String parkName;

    @ApiModelProperty(value = "进场时间")
    private String enterTime;

    @ApiModelProperty(value = "停车时长（分钟）")
    private Long parkingMinutes;

    @ApiModelProperty(value = "超时时长（分钟）")
    private Long overtimeMinutes;

    @ApiModelProperty(value = "管家昵称")
    private String managerNickname;

    @ApiModelProperty(value = "超时级别：1-轻微，2-严重，3-紧急")
    private Integer timeoutLevel;

    /**
     * 获取超时等级
     */
    public Integer getTimeoutLevel() {
        if (overtimeMinutes == null) return 1;
        
        if (overtimeMinutes <= 30) {
            return 1; // 轻微超时（30分钟内）
        } else if (overtimeMinutes <= 120) {
            return 2; // 严重超时（2小时内）
        } else {
            return 3; // 紧急超时（超过2小时）
        }
    }

    /**
     * 获取格式化的超时时长
     */
    public String getFormattedOvertimeText() {
        if (overtimeMinutes == null) return "0分钟";
        
        long hours = overtimeMinutes / 60;
        long minutes = overtimeMinutes % 60;
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }

    /**
     * 获取格式化的停车时长
     */
    public String getFormattedParkingText() {
        if (parkingMinutes == null) return "0分钟";
        
        long hours = parkingMinutes / 60;
        long minutes = parkingMinutes % 60;
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分钟";
        } else {
            return minutes + "分钟";
        }
    }
} 