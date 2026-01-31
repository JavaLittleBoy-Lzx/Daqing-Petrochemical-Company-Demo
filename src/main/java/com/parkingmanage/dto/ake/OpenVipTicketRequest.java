package com.parkingmanage.dto.ake;

import lombok.Data;
import java.util.List;

/**
 * AKE开通VIP票请求DTO
 * 对应接口：OPEN_VIP_TICKET (4.2)
 */
@Data
public class OpenVipTicketRequest {
    
    /** VIP类型名称 */
    private String vipTypeName;
    
    /** 票号 */
    private String ticketNo;
    
    /** 车主姓名 */
    private String carOwner;
    
    /** 电话 */
    private String telphone;
    
    /** 单位 */
    private String company;
    
    /** 部门 */
    private String department;
    
    /** 性别 0-男 1-女 */
    private String sex;
    
    /** 操作人 */
    private String operator;
    
    /** 操作时间 */
    private String operateTime;
    
    /** 原价 */
    private String originalPrice;
    
    /** 折扣价 */
    private String discountPrice;
    
    /** 开通值 */
    private String openValue;
    
    /** 开通车辆数 */
    private String openCarCount;
    
    /** 车辆列表 */
    private List<CarInfo> carList;
    
    /** 时间段列表 */
    private List<TimePeriod> timePeriodList;
    
    @Data
    public static class CarInfo {
    /** 车牌号 */
        private String carNo;
    }
    
    @Data
    public static class TimePeriod {
        /** 开始时间 */
        private String startTime;
        
        /** 结束时间 */
        private String endTime;
    }
}
