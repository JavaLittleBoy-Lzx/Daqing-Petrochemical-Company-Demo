package com.parkingmanage.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车牌识别结果VO
 * @author AI Assistant
 */
@Data
@ApiModel(value = "车牌识别结果", description = "车牌识别API返回结果")
public class PlateRecognitionResult {

    @ApiModelProperty("识别是否成功")
    private boolean success;

    @ApiModelProperty("车牌号码")
    private String plateNumber;

    @ApiModelProperty("车牌颜色")
    private String color;

    @ApiModelProperty("车牌类型")
    private String plateType;

    @ApiModelProperty("识别置信度(0-100)")
    private double confidence;

    @ApiModelProperty("识别来源")
    private String source;

    @ApiModelProperty("错误信息")
    private String errorMessage;

    @ApiModelProperty("识别时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recognizeTime;

    @ApiModelProperty("车牌位置信息")
    private PlateLocation location;

    public PlateRecognitionResult() {
        this.recognizeTime = LocalDateTime.now();
    }

    /**
     * 创建成功结果
     */
    public static PlateRecognitionResult success(String plateNumber, String color, double confidence) {
        PlateRecognitionResult result = new PlateRecognitionResult();
        result.setSuccess(true);
        result.setPlateNumber(plateNumber);
        result.setColor(color);
        result.setConfidence(confidence);
        return result;
    }

    /**
     * 创建失败结果
     */
    public static PlateRecognitionResult error(String errorMessage) {
        PlateRecognitionResult result = new PlateRecognitionResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }

    /**
     * 车牌位置信息
     */
    @Data
    @ApiModel("车牌位置信息")
    public static class PlateLocation {
        @ApiModelProperty("左上角X坐标")
        private int left;

        @ApiModelProperty("左上角Y坐标")
        private int top;

        @ApiModelProperty("宽度")
        private int width;

        @ApiModelProperty("高度")
        private int height;
    }
} 