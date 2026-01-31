package com.parkingmanage.service.well.impl;

import com.parkingmanage.dto.well.TimeRuleInfo;
import com.parkingmanage.dto.well.TimeRuleRequest;
import com.parkingmanage.service.well.TimeRuleService;
import com.parkingmanage.service.well.WellPersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 时段规则服务实现类
 * 管理人员进出时段规则，包括规则缓存、规则匹配和创建
 */
@Slf4j
@Service
public class TimeRuleServiceImpl implements TimeRuleService {

    @Autowired
    private WellPersonService wellPersonService;

    /** 规则缓存：规则名称 -> 规则ID */
    private final Map<String, Integer> ruleCache = new ConcurrentHashMap<>();

    /** 长期规则ID（正式员工/子女工使用） */
    private Integer permanentRuleId;

    /** 长期规则名称 */
    private static final String PERMANENT_RULE_NAME = "长期通行规则";

    /** 正式员工类型列表 */
    private static final List<String> PERMANENT_PERSON_TYPES = Arrays.asList("正式员工", "子女工");

    /** 临时员工类型列表 */
    private static final List<String> TEMPORARY_PERSON_TYPES = Arrays.asList("外来员工", "施工人员");

    /** 日期格式化器 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 服务初始化时加载规则缓存
     */
    @PostConstruct
    public void init() {
        refreshRuleCache();
    }

    @Override
    public void refreshRuleCache() {
        log.info("开始刷新时段规则缓存");
        try {
            List<TimeRuleInfo> rules = wellPersonService.getTimeRuleList();
            ruleCache.clear();
            permanentRuleId = null;
            for (TimeRuleInfo rule : rules) {
                ruleCache.put(rule.getRuleName(), rule.getRuleId());
                
                // 查找长期规则（匹配多种可能的名称）
                if (isPermanentRule(rule.getRuleName())) {
                    permanentRuleId = rule.getRuleId();
                    log.info("找到长期规则: {} -> ID: {}", rule.getRuleName(), rule.getRuleId());
                }
            }
            log.info("时段规则缓存刷新完成，共 {} 条规则，长期规则ID: {}", 
                    ruleCache.size(), permanentRuleId);
        } catch (Exception e) {
            log.error("刷新时段规则缓存失败", e);
        }
    }

    /**
     * 判断是否为长期规则
     */
    private boolean isPermanentRule(String ruleName) {
        if (ruleName == null) {
            return false;
        }
        return PERMANENT_RULE_NAME.equals(ruleName)
                || ruleName.contains("长期")
                || ruleName.contains("永久")
                || ruleName.contains("默认");
    }

    @Override
    public Integer getOrCreateTimeRule(String personType, LocalDate startDate, LocalDate endDate) {
        log.info("获取或创建时段规则，人员类型: {}, 有效期: {} ~ {}", personType, startDate, endDate);
        
        // 正式员工和子女工使用长期规则
        if (PERMANENT_PERSON_TYPES.contains(personType)) {
            return getOrCreatePermanentRule(personType);
        }
        // 外来员工和施工人员根据有效期创建规则
        if (TEMPORARY_PERSON_TYPES.contains(personType)) {
            return getOrCreateTemporaryRule(startDate, endDate);
        }
        // 其他类型默认使用长期规则
        log.warn("未知人员类型[{}]，使用长期规则", personType);
        return getOrCreatePermanentRule(personType);
    }

    /**
     * 获取或创建长期规则
     */
    private Integer getOrCreatePermanentRule(String personType) {
        if (permanentRuleId != null) {
            log.info("人员类型[{}]使用长期规则，规则ID: {}", personType, permanentRuleId);
            return permanentRuleId;
        }
        // 如果没有长期规则，创建一个
        log.info("长期规则不存在，开始创建");
        return createPermanentRule();
    }

    /**
     * 获取或创建临时规则
     */
    private Integer getOrCreateTemporaryRule(LocalDate startDate, LocalDate endDate) {
        // 有效期为空时使用长期规则
        if (startDate == null || endDate == null) {
            log.warn("有效期为空，使用长期规则");
            return getOrCreatePermanentRule("临时人员");
        }
        // 生成规则名称
        String ruleName = generateRuleName(startDate, endDate);
        // 检查缓存中是否存在
        if (ruleCache.containsKey(ruleName)) {
            Integer ruleId = ruleCache.get(ruleName);
            log.debug("使用已有规则[{}]，规则ID: {}", ruleName, ruleId);
            return ruleId;
        }
        // 创建新规则
        log.info("规则[{}]不存在，开始创建", ruleName);
        return createTemporaryRule(ruleName, startDate, endDate);
    }

    @Override
    public String generateRuleName(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return PERMANENT_RULE_NAME;
        }
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();
        if (isCrossYear(startDate, endDate)) {
            return String.format("临时通行规则_%d-%d", startYear, endYear);
        } else {
            return String.format("临时通行规则_%d", startYear);
        }
    }

    @Override
    public boolean isCrossYear(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return startDate.getYear() != endDate.getYear();
    }

    /**
     * 创建长期规则
     */
    private Integer createPermanentRule() {
        log.info("创建长期通行规则: {}", PERMANENT_RULE_NAME);
        
        TimeRuleRequest request = buildTimeRuleRequest(
                PERMANENT_RULE_NAME,
                "PERMANENT_RULE_" + System.currentTimeMillis(),
                "全天通行",
                "2010-01-01",
                "2049-12-31"
        );
        boolean success = wellPersonService.batchInsertOrUpdateTimeRule(Collections.singletonList(request));
        if (success) {
            // 刷新缓存获取新规则ID
            refreshRuleCache();
            Integer ruleId = ruleCache.get(PERMANENT_RULE_NAME);
            log.info("长期规则创建成功，规则ID: {}", ruleId);
            return ruleId;
        }
        log.error("创建长期规则失败");
        return null;
    }

    /**
     * 创建临时规则
     */
    private Integer createTemporaryRule(String ruleName, LocalDate startDate, LocalDate endDate) {
        log.info("创建临时通行规则: {}, 有效期: {} ~ {}", ruleName, startDate, endDate);
        
        TimeRuleRequest request = buildTimeRuleRequest(
                ruleName,
                "TEMP_RULE_" + System.currentTimeMillis(),
                "临时通行时段",
                startDate.format(DATE_FORMATTER),
                endDate.format(DATE_FORMATTER)
        );
        boolean success = wellPersonService.batchInsertOrUpdateTimeRule(Collections.singletonList(request));
        if (success) {
            // 刷新缓存获取新规则ID
            refreshRuleCache();
            Integer ruleId = ruleCache.get(ruleName);
            log.info("临时规则创建成功: {}, 规则ID: {}", ruleName, ruleId);
            return ruleId;
        }
        log.error("创建临时规则失败: {}", ruleName);
        return null;
    }

    /**
     * 构建时段规则请求对象
     */
    private TimeRuleRequest buildTimeRuleRequest(String ruleName, String sourceNo, 
            String itemName, String beginDate, String endDate) {
        TimeRuleRequest request = new TimeRuleRequest();
        request.setRuleName(ruleName);
        request.setSourceNo(sourceNo);
        // 单个时段
        TimeRuleRequest.TimeRuleItem item = new TimeRuleRequest.TimeRuleItem();
        item.setRuleItemName(itemName);
        item.setBeginDate(beginDate);
        item.setEndDate(endDate);
        item.setMonthBegin(1);
        item.setMonthEnd(12);
        item.setWeekBegin(1);
        item.setWeekEnd(7);
        item.setDayBegin(1);
        item.setDayEnd(31);
        item.setTimeBegin1("00:00:00");
        item.setTimeEnd1("23:59:59");
        // 其他时间段留空
        item.setTimeBegin2("");
        item.setTimeEnd2("");
        item.setTimeBegin3("");
        item.setTimeEnd3("");
        item.setTimeBegin4("");
        item.setTimeEnd4("");
        // 添加时段
        request.setItemList(Collections.singletonList(item));
        return request;
    }

    @Override
    public Integer getPermanentRuleId() {
        return permanentRuleId;
    }
}
