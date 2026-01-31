package com.parkingmanage.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 车牌识别请求VO
 * @author AI Assistant
 */
@Data
@ApiModel(value = "车牌识别请求", description = "车牌识别API请求参数")
public class PlateRecognitionRequest {

    @ApiModelProperty(value = "Base64编码的图片数据", required = true)
    private String image;

    @ApiModelProperty("是否支持多车牌识别")
    private boolean multiDetect = false;

    @ApiModelProperty("是否开启车牌遮挡检测功能")
    private boolean detectComplete = false;

    @ApiModelProperty("是否开启车牌PS检测功能")
    private boolean detectRisk = false;

    @ApiModelProperty("识别引擎类型（baidu/tencent/ali）")
    private String engine = "baidu";
} 