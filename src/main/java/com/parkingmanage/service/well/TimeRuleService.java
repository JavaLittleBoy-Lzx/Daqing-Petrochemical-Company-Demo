package com.parkingmanage.service.well;

import java.time.LocalDate;

/**
 * 时段规则服务接口
 * 管理人员进出时段规则
 */
public interface TimeRuleService {

    /**
     * 刷新规则缓存
     * 从威尔系统获取最新的时段规则列表并更新本地缓存
     */
    void refreshRuleCache();

    /**
     * 根据人员类型和有效期获取或创建规则
     * 
     * @param personType 人员类型（正式员工/子女工/外来员工/施工人员）
     * @param startDate 有效期开始日期
     * @param endDate 有效期结束日期
     * @return 规则ID，如果创建失败返回null
     */
    Integer getOrCreateTimeRule(String personType, LocalDate startDate, LocalDate endDate);

    /**
     * 判断是否跨年
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return true表示跨年，false表示不跨年
     */
    boolean isCrossYear(LocalDate startDate, LocalDate endDate);

    /**
     * 生成规则名称
     * 不跨年格式：临时通行规则_2025
     * 跨年格式：临时通行规则_2025-2026
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 规则名称
     */
    String generateRuleName(LocalDate startDate, LocalDate endDate);

    /**
     * 获取长期规则ID
     * 
     * @return 长期规则ID，如果不存在返回null
     */
    Integer getPermanentRuleId();
}
