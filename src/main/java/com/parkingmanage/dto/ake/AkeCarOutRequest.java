package com.parkingmanage.dto.ake;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AKE离场记录请求
 */
@Data
public class AkeCarOutRequest extends AkeRecordRequest {
    
    /** 业务内容 */
    @JsonProperty("biz_content")
    private CarOutBizContent bizContent;
    
    @Data
    public static class CarOutBizContent {
        /** 实收金额 */
        @JsonProperty("actual_receivable")
        private String actualReceivable;
        
        /** 应收金额 */
        @JsonProperty("amount_receivable")
        private String amountReceivable;
        
        /** 车牌号 */
        @JsonProperty("car_license_number")
        private String carLicenseNumber;
        
        /** 纠正置信度 */
        @JsonProperty("correct_confidence")
        private String correctConfidence;
        
        /** 纠正类型 */
        @JsonProperty("correct_type")
        private String correctType;
        
        /** 优惠金额 */
        @JsonProperty("discount_amount")
        private String discountAmount;
        
        /** 优惠名称 */
        @JsonProperty("discount_name")
        private String discountName;
        
        /** 优惠编号 */
        @JsonProperty("discount_no")
        private String discountNo;
        
        /** 优惠验证值 */
        @JsonProperty("discount_validate_value")
        private String discountValidateValue;
        
        /** 进场卡号 */
        @JsonProperty("enter_car_card_number")
        private String enterCarCardNumber;
        
        /** 进场车辆颜色 */
        @JsonProperty("enter_car_color")
        private String enterCarColor;
        
        /** 进场车辆全景图 */
        @JsonProperty("enter_car_full_picture")
        private String enterCarFullPicture;
        
        /** 进场车牌颜色 */
        @JsonProperty("enter_car_license_color")
        private String enterCarLicenseColor;
        
        /** 进场车牌号 */
        @JsonProperty("enter_car_license_number")
        private String enterCarLicenseNumber;
        
        /** 进场车牌图片 */
        @JsonProperty("enter_car_license_picture")
        private String enterCarLicensePicture;
        
        /** 进场车牌类型 */
        @JsonProperty("enter_car_license_type")
        private String enterCarLicenseType;
        
        /** 进场车辆品牌 */
        @JsonProperty("enter_car_logo")
        private String enterCarLogo;
        
        /** 进场车辆类型 */
        @JsonProperty("enter_car_type")
        private String enterCarType;
        
        /** 进场通道 */
        @JsonProperty("enter_channel")
        private String enterChannel;
        
        /** 进场识别置信度 */
        @JsonProperty("enter_recognition_confidence")
        private String enterRecognitionConfidence;
        
        /** 进场速度 */
        @JsonProperty("enter_speed")
        private String enterSpeed;
        
        /** 进场时间 */
        @JsonProperty("enter_time")
        private String enterTime;
        
        /** 进场类型 */
        @JsonProperty("enter_type")
        private String enterType;
        
        /** 入场操作员 */
        @JsonProperty("in_operator_name")
        private String inOperatorName;
        
        /** 入场操作时间 */
        @JsonProperty("in_operator_time")
        private String inOperatorTime;
        
        /** 是否纠正 */
        @JsonProperty("is_correct")
        private String isCorrect;
        
        /** 上次纠正车牌号 */
        @JsonProperty("last_correct_license_number")
        private String lastCorrectLicenseNumber;
        
        /** 上次纠正人 */
        @JsonProperty("last_correct_name")
        private String lastCorrectName;
        
        /** 上次纠正时间 */
        @JsonProperty("last_correct_time")
        private String lastCorrectTime;
        
        /** 上次支付时间 */
        @JsonProperty("last_pay_time")
        private String lastPayTime;
        
        /** 离场卡号 */
        @JsonProperty("leave_car_card_number")
        private String leaveCarCardNumber;
        
        /** 离场车辆颜色 */
        @JsonProperty("leave_car_color")
        private String leaveCarColor;
        
        /** 离场车辆全景图 */
        @JsonProperty("leave_car_full_picture")
        private String leaveCarFullPicture;
        
        /** 离场车牌颜色 */
        @JsonProperty("leave_car_license_color")
        private String leaveCarLicenseColor;
        
        /** 离场车牌号 */
        @JsonProperty("leave_car_license_number")
        private String leaveCarLicenseNumber;
        
        /** 离场车牌图片 */
        @JsonProperty("leave_car_license_picture")
        private String leaveCarLicensePicture;
        
        /** 离场车牌类型 */
        @JsonProperty("leave_car_license_type")
        private String leaveCarLicenseType;
        
        /** 离场车辆品牌 */
        @JsonProperty("leave_car_logo")
        private String leaveCarLogo;
        
        /** 离场车辆类型 */
        @JsonProperty("leave_car_type")
        private String leaveCarType;
        
        /** 离场通道 */
        @JsonProperty("leave_channel")
        private String leaveChannel;
        
        /** 离场通道名称 */
        @JsonProperty("leave_channel_name")
        private String leaveChannelName;
        
        /** 离场识别置信度 */
        @JsonProperty("leave_recognition_confidence")
        private String leaveRecognitionConfidence;
        
        /** 离场速度 */
        @JsonProperty("leave_speed")
        private String leaveSpeed;
        
        /** 离场时间 */
        @JsonProperty("leave_time")
        private String leaveTime;
        
        /** 离场类型 */
        @JsonProperty("leave_type")
        private String leaveType;
        
        /** 出场操作员 */
        @JsonProperty("out_operator_name")
        private String outOperatorName;
        
        /** 出场操作时间 */
        @JsonProperty("out_operator_time")
        private String outOperatorTime;
        
        /** 停车场序号 */
        @JsonProperty("parking_lot_seq")
        private String parkingLotSeq;
        
        /** 支付来源 */
        @JsonProperty("pay_origin")
        private String payOrigin;
        
        /** 支付状态 */
        @JsonProperty("pay_status")
        private String payStatus;
        
        /** 支付方式 */
        @JsonProperty("payment_mode")
        private String paymentMode;
        
        /** 记录编号 */
        @JsonProperty("record_number")
        private String recordNumber;
        
        /** 记录类型 */
        @JsonProperty("record_type")
        private String recordType;
        
        /** 备注 */
        @JsonProperty("remark")
        private String remark;
        
        /** 停车时长 */
        @JsonProperty("stopping_time")
        private String stoppingTime;
        
        /** 收费员姓名 */
        @JsonProperty("toll_collector_name")
        private String tollCollectorName;
        
        /** 收费时间 */
        @JsonProperty("toll_collector_time")
        private String tollCollectorTime;
        
        /** 总金额 */
        @JsonProperty("total_amount")
        private String totalAmount;
    }
}
