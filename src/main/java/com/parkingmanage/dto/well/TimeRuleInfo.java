package com.parkingmanage.dto.well;

import lombok.Data;
import java.util.List;

/**
 * 威尔时段规则信息DTO
 * 对应接口：/api-gating/api-gating/open-gating-time-rule/list 响应
 */
@Data
public class TimeRuleInfo {
    
    /** 规则ID */
    private Integer ruleId;
    
    /** 规则名称 */
    private String ruleName;
    
    /** 规则编号 */
    private String sourceNo;
    
    /** 规则时段详情 */
    private List<TimeRuleItem> itemList;
    
    /**
     * 时段规则项
     */
    @Data
    public static class TimeRuleItem {
        /** 时段ID */
        private Integer id;
        
        /** 时段名称 */
        private String ruleItemName;
        
        /** 有效期开始 */
        private String beginDate;
        
        /** 有效期结束 */
        private String endDate;
        
        /** 有效开始月份 */
        private Integer monthBegin;
        
        /** 有效结束月份 */
        private Integer monthEnd;
        
        /** 有效开始星期 */
        private Integer weekBegin;
        
        /** 有效结束星期 */
        private Integer weekEnd;
        
        /** 每月有效开始日 */
        private Integer dayBegin;
        
        /** 每月有效结束日 */
        private Integer dayEnd;
        
        /** 开始时间1 */
        private String timeBegin1;
        
        /** 结束时间1 */
        private String timeEnd1;
    }
}
