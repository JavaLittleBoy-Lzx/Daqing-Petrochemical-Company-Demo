package com.parkingmanage.dto.ake;

import lombok.Data;

/**
 * AKE添加访客车辆请求DTO
 * 对应接口：ADD_VISITOR_CAR
 */
@Data
public class AddVisitorCarRequest {

    /** 车牌号 */
    private String carCode;

    /** 车主姓名 */
    private String owner;

    /** 访客类型名称（如：来访车辆（化工西门）） */
    private String visitName;

    /** 手机号 */
    private String phonenum;

    /** 访问原因 */
    private String reason;

    /** 操作人 */
    private String operator;

    /** 操作时间 */
    private String operateTime;

    /** 访问时间 */
    private VisitTime visitTime;

    @Data
    public static class VisitTime {
        /** 开始时间 */
        private String startTime;

        /** 结束时间 */
        private String endTime;
    }
}
