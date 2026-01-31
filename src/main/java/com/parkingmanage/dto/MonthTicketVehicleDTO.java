package com.parkingmanage.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 月票车辆综合信息DTO
 * 用于搜索结果展示，整合了月票信息、车主信息、车辆状态等
 */
@Data
public class MonthTicketVehicleDTO {
    
    /**
     * 车牌号
     */
    private String plateNumber;
    
    /**
     * 月票名称
     */
    private String ticketName;
    
    /**
     * 车主姓名
     */
    private String ownerName;
    
    /**
     * 车主电话
     */
    private String ownerPhone;
    
    /**
     * 车场名称
     */
    private String parkName;
    
    /**
     * 车场代码
     */
    private String parkCode;
    
    /**
     * 车位号
     */
    private String parkingSpot;
    
    /**
     * 有效状态 (1-有效, 4-过期)
     */
    private Integer validStatus;
    
    /**
     * 是否冻结 (0-正常, 1-冻结)
     */
    private Integer isFrozen;
    
    /**
     * 是否在场
     */
    private Boolean isInPark;
    
    /**
     * 最后入场时间
     */
    private String lastEntryTime;
    
    /**
     * 预约记录数
     */
    private Integer appointmentCount;
    
    /**
     * 违规记录数
     */
    private Integer violationCount;
    
    /**
     * 信用分数
     */
    private Integer creditScore;
    
    /**
     * 月票开始时间
     */
    private String startTime;
    
    /**
     * 月票结束时间
     */
    private String endTime;
    
    /**
     * 备注信息1
     */
    private String remark1;
    
    /**
     * 备注信息2
     */
    private String remark2;
    
    /**
     * 备注信息3
     */
    private String remark3;
    
    /**
     * 搜索相关度评分 (用于排序)
     */
    private Double relevanceScore;
    
    /**
     * 月票ID
     */
    private Long monthTicketId;
} 