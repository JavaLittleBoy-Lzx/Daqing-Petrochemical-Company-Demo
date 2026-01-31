package com.parkingmanage.dto.well;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * 威尔临时时段授权请求DTO
 * 对应接口：/api-gating/api-gating/open-gating-single-grant/batch/insert-or-update
 *
 * 用于外来员工、施工人员等临时人员的门禁授权
 * 每个人员可以有独立的授权时间段
 */
@Data
public class WellSingleGrantRequest {

    /** 人员id（没有时传工号） */
    @JSONField(ordinal = 1)
    private Integer userId;

    /** 人员工号 */
    @JSONField(ordinal = 2)
    private String userNo;

    /** 门id */
    @JSONField(ordinal = 3)
    private Integer doorId;

    /** 生效方式 1:正向(默认) 2:反向(对应时段内无权限) */
    @JSONField(ordinal = 4)
    private Integer effectWay;

    /** 时间段模式：1:连续时段 2:每日固定时段 */
    @JSONField(ordinal = 5)
    private Integer timeModel;

    /** 开始时间(yyyy-MM-dd hh:mi:ss) */
    @JSONField(ordinal = 6)
    private String beginTime;

    /** 结束时间(yyyy-MM-dd hh:mi:ss) */
    @JSONField(ordinal = 7)
    private String endTime;

    /** 授权编号 */
    @JSONField(ordinal = 8)
    private String sourceNo;
}
