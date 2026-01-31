package com.parkingmanage.service.well;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.dto.well.WellGateRecordRequest;
import com.parkingmanage.dto.well.WellGateRecordResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 威尔门禁记录服务
 * 负责查询威尔门禁系统的进出记录
 */
@Slf4j
@Service
public class WellGateRecordService {

    @Value("${well.api.base-url}")
    private String baseUrl;

    @Value("${well.api.gate-record-url:/api-gating/api-gating/open-dev-record/list}")
    private String gateRecordUrl;

    @Value("${well.api.app-key}")
    private String appKey;

    @Value("${well.api.sign}")
    private String sign;

    @Value("${well.api.version}")
    private String version;

    /**
     * 构建威尔接口请求头
     * 不包含认证参数，只保留基本的Content-Type
     *
     * @return 请求头Map
     */
    private Map<String, String> buildWellHeaders() {
        Map<String, String> headers = new HashMap<>();
        // 不添加 appKey、sign、timestamp、version 等认证参数
        // HttpClientUtil.doPostJsonWithHeaders 会自动添加 Content-Type
        return headers;
    }

    /**
     * 查询门禁记录列表
     * 
     * @param request 查询请求参数
     * @return 门禁记录列表
     */
    public List<WellGateRecordResponse> getGateRecordList(WellGateRecordRequest request) {
        if (request == null) {
            log.warn("查询参数为空");
            return new ArrayList<>();
        }

        // 构建完整URL
        String url = baseUrl + gateRecordUrl;
        
        // 构建请求Body参数
        Map<String, Object> bodyParams = new HashMap<>();
        if (request.getPageIndex() != null) {
            bodyParams.put("pageIndex", request.getPageIndex());
        }
        if (request.getPageSize() != null) {
            bodyParams.put("pageSize", request.getPageSize());
        }
        if (request.getBeginTimestamp() != null) {
            bodyParams.put("beginTimestamp", request.getBeginTimestamp());
        }
        if (request.getEndTimestamp() != null) {
            bodyParams.put("endTimestamp", request.getEndTimestamp());
        }

        // 将参数转换为JSON字符串
        String jsonBody = JSON.toJSONString(bodyParams);
        
        log.info("调用威尔门禁记录接口: {}", url);
        log.info("请求URL: {}", url);
        log.info("请求Body: {}", jsonBody);
        log.info("请求Headers: {}", buildWellHeaders());
        log.debug("查询参数: beginTimestamp={}, endTimestamp={}, pageSize={}",
                request.getBeginTimestamp(), request.getEndTimestamp(), request.getPageSize());

        try {
            // 发送POST请求（参数在Body中）
            String response = HttpClientUtil.doPostJsonWithHeaders(url, jsonBody, buildWellHeaders());
            log.info("威尔门禁记录接口响应: {}", response);

            return parseRecordListResponse(response);
        } catch (Exception e) {
            log.error("调用威尔门禁记录接口失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 查询指定时间范围的门禁记录
     * 
     * @param beginTimestamp 开始时间戳（13位毫秒）
     * @param endTimestamp 结束时间戳（13位毫秒）
     * @return 门禁记录列表
     */
    public List<WellGateRecordResponse> getGateRecordsByTimeRange(Long beginTimestamp, Long endTimestamp) {
        WellGateRecordRequest request = new WellGateRecordRequest();
        request.setPageIndex(1);
        request.setPageSize(10000); // 最大条数
        request.setBeginTimestamp(beginTimestamp);
        request.setEndTimestamp(endTimestamp);

        return getGateRecordList(request);
    }

    /**
     * 查询最近N分钟的门禁记录
     * 
     * @param minutes 分钟数
     * @return 门禁记录列表
     */
    public List<WellGateRecordResponse> getRecentGateRecords(int minutes) {
        long now = System.currentTimeMillis();
        long beginTime = now - (minutes * 60 * 1000L);
        
        log.info("查询最近{}分钟的门禁记录: {} ~ {}", minutes, beginTime, now);
        
        return getGateRecordsByTimeRange(beginTime, now);
    }

    /**
     * 解析门禁记录列表响应
     */
    private List<WellGateRecordResponse> parseRecordListResponse(String response) {
        if (!StringUtils.hasText(response)) {
            log.warn("门禁记录响应为空");
            return new ArrayList<>();
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            String msg = jsonResponse.getString("msg");

            // 威尔接口成功状态码为0或600
            if (code != null && (code == 0 || code == 600)) {
                List<WellGateRecordResponse> records = jsonResponse.getJSONArray("data")
                        .toJavaList(WellGateRecordResponse.class);
                log.info("查询门禁记录成功，共{}条", records.size());
                return records;
            } else {
                log.warn("查询门禁记录失败，code: {}, msg: {}", code, msg);
                return new ArrayList<>();
            }
        } catch (Exception e) {
            log.error("解析门禁记录响应失败", e);
            return new ArrayList<>();
        }
    }
}
