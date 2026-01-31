package com.parkingmanage.dto.well;

import lombok.Data;
import java.util.List;

/**
 * 威尔时段规则请求DTO
 * 对应接口：/api-gating/api-gating/open-gating-time-rule/batch/insert-or-update
 */
@Data
public class TimeRuleRequest {
    
    /** 规则名称 */
    private String ruleName;
    
    /** 规则编号（用于判断新增或修改） */
    private String sourceNo;
    
    /** 规则时段详情 */
    private List<TimeRuleItem> itemList;
    
    /**
     * 时段规则项
     */
    @Data
    public static class TimeRuleItem {
        /** 时段名称 */
        private String ruleItemName;
        
        /** 有效期开始(yyyy-MM-dd) */
        private String beginDate;
        
        /** 有效期结束(yyyy-MM-dd) */
        private String endDate;
        
        /** 有效开始月份(1-12) */
        private Integer monthBegin;
        
        /** 有效结束月份(1-12) */
        private Integer monthEnd;
        
        /** 有效开始星期(1-7) */
        private Integer weekBegin;
        
        /** 有效结束星期(1-7) */
        private Integer weekEnd;
        
        /** 每月有效开始日(1-31) */
        private Integer dayBegin;
        
        /** 每月有效结束日(1-31) */
        private Integer dayEnd;
        
        /** 开始时间1（HH:mm:ss） */
        private String timeBegin1;
        
        /** 结束时间1（HH:mm:ss） */
        private String timeEnd1;
        
        /** 开始时间2 */
        private String timeBegin2;
        
        /** 结束时间2 */
        private String timeEnd2;
        
        /** 开始时间3 */
        private String timeBegin3;
        
        /** 结束时间3 */
        private String timeEnd3;
        
        /** 开始时间4 */
        private String timeBegin4;
        
        /** 结束时间4 */
        private String timeEnd4;
    }
}
