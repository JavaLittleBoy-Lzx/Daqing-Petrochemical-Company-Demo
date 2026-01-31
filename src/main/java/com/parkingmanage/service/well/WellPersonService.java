package com.parkingmanage.service.well;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.dto.well.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 威尔人员服务
 * 负责调用威尔门禁系统接口
 */
@Slf4j
@Service
public class WellPersonService {

    @Value("${well.api.base-url}")
    private String baseUrl;

    @Value("${well.api.person-url}")
    private String personUrl;

    @Value("${well.api.person-delete-url}")
    private String personDeleteUrl;

    @Value("${well.api.face-url}")
    private String faceUrl;

    @Value("${well.api.time-rule-list-url}")
    private String timeRuleListUrl;

    @Value("${well.api.time-rule-url}")
    private String timeRuleUrl;

    @Value("${well.api.grant-url}")
    private String grantUrl;

    @Value("${well.api.single-grant-url:/api-gating/api-gating/open-gating-single-grant/batch/insert-or-update}")
    private String singleGrantUrl;

    @Value("${well.api.door-list-url:/api-gating/api-gating/open-gating-door/doorList}")
    private String doorListUrl;

    @Value("${well.api.app-key}")
    private String appKey;

    @Value("${well.api.sign}")
    private String sign;

    @Value("${well.api.version}")
    private String version;

    /**
     * 构建威尔接口请求头
     * 包含认证参数：appKey、sign、timestamp、version
     *
     * @return 请求头Map
     */
    private java.util.Map<String, String> buildWellHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("appKey", appKey);
        headers.put("sign", sign);
        headers.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000)); // 当前时间戳（秒）
        headers.put("version", version);

        log.debug("威尔接口请求头: {}", headers);

        return headers;
    }

    /**
     * 批量新增或修改人员（员工编号sourceNo为唯一约束）
     * 支持分批处理，避免请求体过大
     *
     * @param userList 人员列表
     * @return 是否全部成功
     */
    public boolean batchInsertOrUpdatePerson(List<WellPersonRequest> userList) {
        if (userList == null || userList.isEmpty()) {
            log.warn("人员列表为空，跳过同步");
            return true;
        }

        String url = baseUrl + personUrl;
        log.info("调用威尔人员接口: {}, 人员总数: {}", url, userList.size());

        // 分批处理，每批最多50条（人员信息数据量适中）
        int batchSize = 50;
        int totalBatches = (userList.size() + batchSize - 1) / batchSize;
        boolean allSuccess = true;

        for (int i = 0; i < userList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, userList.size());
            List<WellPersonRequest> batch = userList.subList(i, end);
            int batchNum = i / batchSize + 1;

            log.info("发送人员批次: {}/{}, 数量: {}", batchNum, totalBatches, batch.size());

            try {
                String requestJson = JSON.toJSONString(batch);
                log.debug("请求参数: {}", requestJson);

                String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
                log.info("威尔人员接口响应[批次{}]: {}", batchNum, response);

                boolean success = parseResponse(response, "人员同步-批次" + batchNum);
                if (!success) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("调用威尔人员接口失败[批次{}]: {}", batchNum, e.getMessage(), e);
                allSuccess = false;
            }

            // 批次之间暂停一下，避免请求过快
            if (batchNum < totalBatches) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return allSuccess;
    }

    /**
     * 批量删除人员（使用RYID/员工编号）
     * 用于处理注销状态（DQZT="D"）的人员
     *
     * 接口请求格式：["ryid1", "ryid2", ...]
     *
     * @param sourceNos 人员RYID列表（对应Oracle的RYID字段）
     * @return 是否成功
     */
    public boolean batchDeletePerson(List<String> sourceNos) {
        if (sourceNos == null || sourceNos.isEmpty()) {
            log.warn("删除人员列表为空，跳过删除");
            return true;
        }

        String url = baseUrl + personDeleteUrl;
        log.info("调用威尔删除人员接口: {}, 人员数量: {}", url, sourceNos.size());

        try {
            String requestJson = JSON.toJSONString(sourceNos);
            log.debug("请求参数: {}", requestJson);

            String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
            log.info("威尔删除人员接口响应: {}", response);

            return parseResponse(response, "人员删除");
        } catch (Exception e) {
            log.error("调用威尔删除人员接口失败", e);
            return false;
        }
    }

    /**
     * 批量新增人脸照片
     * 支持分批处理，避免请求体过大
     *
     * @param faceList 人脸列表
     * @return 是否全部成功
     */
    public boolean batchInsertFace(List<WellFaceRequest> faceList) {
        if (faceList == null || faceList.isEmpty()) {
            log.warn("人脸列表为空，跳过同步");
            return true;
        }

        String url = baseUrl + faceUrl;
        log.info("调用威尔人脸接口: {}, 人脸总数: {}", url, faceList.size());

        // 分批处理，每批最多10条（因为Base64数据量大）
        int batchSize = 10;
        int totalBatches = (faceList.size() + batchSize - 1) / batchSize;
        boolean allSuccess = true;

        for (int i = 0; i < faceList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, faceList.size());
            List<WellFaceRequest> batch = faceList.subList(i, end);
            int batchNum = i / batchSize + 1;

            log.info("发送人脸批次: {}/{}, 数量: {}", batchNum, totalBatches, batch.size());

            try {
                String requestJson = JSON.toJSONString(batch);

                String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
                log.info("威尔人脸接口响应[批次{}]: {}", batchNum, response);

                boolean success = parseResponse(response, "人脸同步-批次" + batchNum);
                if (!success) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("调用威尔人脸接口失败[批次{}]: {}", batchNum, e.getMessage(), e);
                allSuccess = false;
            }

            // 批次之间暂停一下，避免请求过快
            if (batchNum < totalBatches) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return allSuccess;
    }

    /**
     * 批量新增或修改人-门授权
     * 
     * @param grantList 授权列表
     * @return 是否成功
     */
    public boolean batchInsertOrUpdateGrant(List<WellGrantRequest> grantList) {
        if (grantList == null || grantList.isEmpty()) {
            log.warn("授权列表为空，跳过同步");
            return true;
        }

        String url = baseUrl + grantUrl;
        log.info("调用威尔授权接口: {}, 授权数量: {}", url, grantList.size());

        try {
            String requestJson = JSON.toJSONString(grantList);
            log.debug("请求参数: {}", requestJson);

            String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
            log.info("威尔授权接口响应: {}", response);

            return parseResponse(response, "授权同步");
        } catch (Exception e) {
            log.error("调用威尔授权接口失败", e);
            return false;
        }
    }

    /**
     * 获取时段规则列表
     * 
     * @return 时段规则列表
     */
    public List<TimeRuleInfo> getTimeRuleList() {
        String url = baseUrl + timeRuleListUrl;
        log.info("调用威尔时段规则列表接口: {}", url);

        try {
            String response = HttpClientUtil.doGetWithHeaders(url, buildWellHeaders());
            log.info("威尔时段规则列表响应: {}", response);

            if (!StringUtils.hasText(response)) {
                return new ArrayList<>();
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            
            // 威尔接口成功状态码为600
            if (code == null || code != 600) {
                log.warn("获取时段规则列表失败，code: {}", code);
                return new ArrayList<>();
            }

            return jsonResponse.getJSONArray("data").toJavaList(TimeRuleInfo.class);
        } catch (Exception e) {
            log.error("调用威尔时段规则列表接口失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 批量新增或修改临时时段授权
     * 支持分批处理，避免请求体过大
     * 用于外来员工、施工人员等临时人员的门禁授权
     * 每个人员可以有独立的授权时间段，无需创建时段规则
     *
     * 接口：/api-gating/api-gating/open-gating-single-grant/batch/insert-or-update
     *
     * @param singleGrantList 临时授权列表
     * @return 是否全部成功
     */
    public boolean batchInsertOrUpdateSingleGrant(List<WellSingleGrantRequest> singleGrantList) {
        if (singleGrantList == null || singleGrantList.isEmpty()) {
            log.warn("临时授权列表为空，跳过同步");
            return true;
        }

        String url = baseUrl + singleGrantUrl;
        log.info("调用威尔临时授权接口: {}, 授权总数: {}", url, singleGrantList.size());

        // 分批处理，每批最多50条（临时授权数据相对较小）
        int batchSize = 50;
        int totalBatches = (singleGrantList.size() + batchSize - 1) / batchSize;
        boolean allSuccess = true;

        for (int i = 0; i < singleGrantList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, singleGrantList.size());
            List<WellSingleGrantRequest> batch = singleGrantList.subList(i, end);
            int batchNum = i / batchSize + 1;

            log.info("发送临时授权批次: {}/{}, 数量: {}", batchNum, totalBatches, batch.size());

            try {
                String requestJson = JSON.toJSONString(batch);
                log.debug("请求参数: {}", requestJson);

                String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
                log.info("威尔临时授权接口响应[批次{}]: {}", batchNum, response);

                boolean success = parseResponse(response, "临时授权同步-批次" + batchNum);
                if (!success) {
                    allSuccess = false;
                }
            } catch (Exception e) {
                log.error("调用威尔临时授权接口失败[批次{}]: {}", batchNum, e.getMessage(), e);
                allSuccess = false;
            }

            // 批次之间暂停一下，避免请求过快
            if (batchNum < totalBatches) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return allSuccess;
    }

    /**
     * 批量新增或修改时段规则
     * 
     * @param ruleList 规则列表
     * @return 是否成功
     */
    public boolean batchInsertOrUpdateTimeRule(List<TimeRuleRequest> ruleList) {
        if (ruleList == null || ruleList.isEmpty()) {
            log.warn("规则列表为空，跳过同步");
            return true;
        }

        String url = baseUrl + timeRuleUrl;
        log.info("调用威尔时段规则接口: {}, 规则数量: {}", url, ruleList.size());

        try {
            String requestJson = JSON.toJSONString(ruleList);
            log.debug("请求参数: {}", requestJson);
            
            String response = HttpClientUtil.doPostJsonWithHeaders(url, requestJson, buildWellHeaders());
            log.info("威尔时段规则接口响应: {}", response);

            return parseResponse(response, "时段规则同步");
        } catch (Exception e) {
            log.error("调用威尔时段规则接口失败", e);
            return false;
        }
    }

    /**
     * 解析威尔接口响应
     *
     * 成功状态码：0 或 600
     */
    private boolean parseResponse(String response, String operation) {
        if (!StringUtils.hasText(response)) {
            log.warn("{}响应为空", operation);
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            String msg = jsonResponse.getString("msg");

            // 威尔接口成功状态码为0或600
            if (code != null && (code == 0 || code == 600)) {
                log.info("{}成功", operation);
                return true;
            } else {
                log.warn("{}失败，code: {}, msg: {}", operation, code, msg);
                return false;
            }
        } catch (Exception e) {
            log.error("解析{}响应失败", operation, e);
            return false;
        }
    }

    /**
     * 获取门禁列表
     * 调用接口：/api-gating/api-gating/open-gating-door/doorList
     * 
     * @return 门禁信息列表
     */
    public List<DoorInfo> getDoorList() {
        String url = baseUrl + doorListUrl;
        log.info("调用威尔门禁列表接口: {}", url);

        try {
            String response = HttpClientUtil.doGetWithHeaders(url, buildWellHeaders());
            log.debug("威尔门禁列表响应: {}", response);

            if (!StringUtils.hasText(response)) {
                log.warn("门禁列表响应为空");
                return new ArrayList<>();
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            Integer code = jsonResponse.getInteger("code");
            
            // 威尔接口成功状态码为600
            if (code == null || code != 600) {
                log.warn("获取门禁列表失败，code: {}", code);
                return new ArrayList<>();
            }

            List<DoorInfo> doorList = jsonResponse.getJSONArray("data").toJavaList(DoorInfo.class);
            log.info("获取到 {} 个门禁", doorList.size());
            return doorList;
        } catch (Exception e) {
            log.error("调用威尔门禁列表接口失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 根据大门名称查找匹配的门禁ID列表
     * 使用模糊匹配：如果placeName包含gateName，则认为匹配
     * 
     * @param gateName 大门名称（如"化工西门"）
     * @param doorList 门禁列表
     * @return 匹配的门禁ID列表
     */
    public List<Integer> findDoorIdsByGateName(String gateName, List<DoorInfo> doorList) {
        List<Integer> matchedDoorIds = new ArrayList<>();
        
        if (gateName == null || gateName.trim().isEmpty() || doorList == null) {
            return matchedDoorIds;
        }
        
        String searchName = gateName.trim();
        
        for (DoorInfo door : doorList) {
            String placeName = door.getPlaceName();
            if (placeName != null && placeName.contains(searchName)) {
                matchedDoorIds.add(door.getDoorId());
                log.debug("门禁匹配成功: gateName={}, placeName={}, doorId={}", 
                        searchName, placeName, door.getDoorId());
            }
        }
        
        return matchedDoorIds;
    }

    /**
     * 门禁信息
     */
    @lombok.Data
    public static class DoorInfo {
        /** 门禁ID */
        private Integer doorId;
        /** 门禁名称 */
        private String doorName;
        /** 位置名称（用于匹配大门名称） */
        private String placeName;
        /** 设备ID */
        private Integer deviceId;
        /** 设备名称 */
        private String deviceName;
        /** 门禁类型 */
        private Integer doorType;
        /** 门禁状态 */
        private Integer doorState;
    }
}
