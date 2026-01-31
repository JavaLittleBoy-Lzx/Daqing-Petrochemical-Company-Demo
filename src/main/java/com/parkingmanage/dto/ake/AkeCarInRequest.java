package com.parkingmanage.dto.ake;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * AKE进场记录请求
 */
@Data
public class AkeCarInRequest extends AkeRecordRequest {
    
    /** 业务内容 */
    @JsonProperty("biz_content")
    private CarInBizContent bizContent;
    
    @Data
    public static class CarInBizContent {
        /** 车牌号 */
        @JsonProperty("car_license_number")
        private String carLicenseNumber;
        
        /** 纠正置信度 */
        @JsonProperty("correct_confidence")
        private String correctConfidence;
        
        /** 纠正类型 */
        @JsonProperty("correct_type")
        private String correctType;
        
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
        
        /** 进场通道名称 */
        @JsonProperty("enter_channel_name")
        private String enterChannelName;
        
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
        
        /** 停车场序号 */
        @JsonProperty("parking_lot_seq")
        private String parkingLotSeq;
        
        /** 记录编号 */
        @JsonProperty("record_number")
        private String recordNumber;
        
        /** 记录类型 */
        @JsonProperty("record_type")
        private String recordType;
        
        /** 备注 */
        @JsonProperty("remark")
        private String remark;
    }
}
