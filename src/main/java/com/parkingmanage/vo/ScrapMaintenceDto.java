package com.parkingmanage.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @PROJECT_NAME: parkingmanage
 * @PACKAGE_NAME: com.parkingmanage.vo
 * @NAME: ScrapMaintenceDto
 * @author:yuli
 * @DATE: 2022/3/8 18:21
 * @description: 报废审核提交报修申请
 */
@Data
public class ScrapMaintenceDto implements Serializable {
    /**
     * Scrap
     */
    private Integer scrapId;


    @ApiModelProperty(value = "审批意见")
    private String audiusReason;

    private Integer deviceStatus;
    /**
     * Maintence
     */
    @ApiModelProperty(value = "维修人")
    private Integer maintenanceUserId;
    @ApiModelProperty(value = "报修人")
    private String repairmanUserId;
    @ApiModelProperty(value = "障描述")
    private String faultDescription;
    @ApiModelProperty(value = "备注")
    private String remarks;
}
