package com.parkingmanage.service.sync.impl;

import com.parkingmanage.dto.PersonSyncResult;
import com.parkingmanage.dto.SyncResult;
import com.parkingmanage.dto.VehicleSyncResult;
import com.parkingmanage.dto.ake.AddBlacklistCarRequest;
import com.parkingmanage.dto.ake.OpenVipTicketRequest;
import com.parkingmanage.dto.well.WellFaceRequest;
import com.parkingmanage.dto.well.WellGrantRequest;
import com.parkingmanage.dto.well.WellPersonRequest;
import com.parkingmanage.dto.well.WellSingleGrantRequest;
import com.parkingmanage.entity.GroupedVehicleInfo;
import com.parkingmanage.entity.OraclePersonInfo;
import com.parkingmanage.entity.OracleVehicleInfo;
import com.parkingmanage.service.ake.AkeVipService;
import com.parkingmanage.service.oracle.OracleDataService;
import com.parkingmanage.service.sync.DataSyncService;
import com.parkingmanage.service.well.TimeRuleService;
import com.parkingmanage.service.well.WellPersonService;
import com.parkingmanage.util.VehicleGroupingUtil;
import com.parkingmanage.util.VipPermissionUtil;
import com.parkingmanage.util.VipTypeMatcherUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 数据同步主服务实现类
 * 协调各服务完成数据同步
 * 
 * Requirements: 7.1, 7.2
 */
@Slf4j
@Service
public class DataSyncServiceImpl implements DataSyncService {

    @Autowired
    private OracleDataService oracleDataService;

    @Autowired
    private WellPersonService wellPersonService;

    @Autowired
    private TimeRuleService timeRuleService;

    @Autowired
    private AkeVipService akeVipService;

    @Value("${sync.last-sync-time-file:./data/last-sync-time.txt}")
    private String lastSyncTimeFile;

    @Value("${well.default-org-source-no:001}")
    private String defaultOrgSourceNo;

    @Value("${well.huagong-ximen-door-ids}")
    private String huagongXimenDoorIds;
    
    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    /** 同步运行状态标志 */
    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    /** 日期时间格式化器 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public SyncResult executeSync() {
        log.info("========== 开始执行数据同步 ==========");
        long startTime = System.currentTimeMillis();
        
        SyncResult result = new SyncResult();
        result.setSyncTime(LocalDateTime.now());
        
        // 检查是否已有同步任务在运行
        if (!syncRunning.compareAndSet(false, true)) {
            log.warn("同步任务正在运行中，跳过本次执行");
            result.setSuccess(false);
            result.setErrorMessage("同步任务正在运行中");
            return result;
        }

        try {
            // 1. 同步人员数据
            log.info(">>> 开始同步人员数据");
            PersonSyncResult personResult = syncPersonData();
            result.setPersonTotal(personResult.getTotal());
            result.setPersonSuccess(personResult.getSuccess());
            result.setPersonFailed(personResult.getFailed());
            // 添加人员失败记录
            if (personResult.getFailedRecords() != null) {
                for (PersonSyncResult.FailedRecord record : personResult.getFailedRecords()) {
                    result.addFailedRecord(String.format("人员[%s-%s]%s失败: %s", 
                            record.getEmployeeNo(), record.getName(), 
                            record.getOperation(), record.getReason()));
                }
            }

            // 2. 同步车辆数据
            log.info(">>> 开始同步车辆数据");
            VehicleSyncResult vehicleResult = syncVehicleData();
            result.setVehicleTotal(vehicleResult.getTotal());
            result.setVehicleSuccess(vehicleResult.getSuccess());
            result.setVehicleFailed(vehicleResult.getFailed());
            result.setBlacklistTotal(vehicleResult.getBlacklistSuccess() + vehicleResult.getBlacklistFailed());
            result.setBlacklistSuccess(vehicleResult.getBlacklistSuccess());
            
            // 添加车辆失败记录
            if (vehicleResult.getFailedRecords() != null) {
                for (VehicleSyncResult.FailedRecord record : vehicleResult.getFailedRecords()) {
                    result.addFailedRecord(String.format("车辆[%s-%s]%s失败: %s", 
                            record.getPlateNumber(), record.getOwnerName(), 
                            record.getOperation(), record.getReason()));
                }
            }

            // 3. 更新同步时间
            updateLastSyncTime(result.getSyncTime());

            result.setSuccess(true);
            log.info("========== 数据同步完成 ==========");
            
        } catch (Exception e) {
            log.error("数据同步异常", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        } finally {
            syncRunning.set(false);
            result.setDuration(System.currentTimeMillis() - startTime);
            
            // 输出同步统计
            log.info("同步统计: 人员[总数:{}, 成功:{}, 失败:{}], 车辆[总数:{}, 成功:{}, 失败:{}], 黑名单[总数:{}], 耗时:{}ms",
                    result.getPersonTotal(), result.getPersonSuccess(), result.getPersonFailed(),
                    result.getVehicleTotal(), result.getVehicleSuccess(), result.getVehicleFailed(),
                    result.getBlacklistTotal(), result.getDuration());
        }

        return result;
    }


    @Override
    public PersonSyncResult syncPersonData() {
        PersonSyncResult result = new PersonSyncResult();

        try {
            // 1. 获取上次同步时间
            LocalDateTime lastSyncTime = getLastSyncTime();
            log.info("人员同步 - 上次同步时间: {}", lastSyncTime);

            // 2. 从Oracle获取最新人员数据
            List<OraclePersonInfo> persons = oracleDataService.getLatestPersonData(lastSyncTime);
            result.setTotal(persons.size());

            log.info("人员同步 - 获取到 {} 条人员数据", persons.size());

            // 3. 分离正常人员和注销人员
            List<OraclePersonInfo> activePersons = new ArrayList<>();
            List<OraclePersonInfo> deletedPersons = new ArrayList<>();

            for (OraclePersonInfo person : persons) {
                // dqzt 字段存储的是 DQZT（当前状态代码），"D"表示注销状态
                // remark 字段存储的是 DQZTNAME（当前状态名称），如"注销"
                if ("D".equals(person.getDqzt())) {
                    deletedPersons.add(person);
                } else {
                    activePersons.add(person);
                }
            }

            log.info("人员同步 - 正常人员: {} 条, 注销人员: {} 条", activePersons.size(), deletedPersons.size());

            // 4. 处理注销人员（调用威尔删除接口）
            if (!deletedPersons.isEmpty()) {
                log.info("人员同步 - 开始删除 {} 条注销人员", deletedPersons.size());
                List<String> sourceNos = new ArrayList<>();

                for (OraclePersonInfo person : deletedPersons) {
                    // employeeNo 对应 Oracle 的 RYID 字段
                    sourceNos.add(person.getEmployeeNo());
                    log.info("人员同步 - 标记删除: RYID={}, 姓名={}, DQZT={}",
                            person.getEmployeeNo(), person.getName(), person.getDqzt());
                }

                try {
                    boolean deleteSuccess = wellPersonService.batchDeletePerson(sourceNos);
                    if (deleteSuccess) {
                        log.info("人员同步 - 注销人员删除成功，数量: {}", deletedPersons.size());
                        // 删除成功计入成功总数
                        result.setSuccess(result.getSuccess() + deletedPersons.size());
                    } else {
                        log.warn("人员同步 - 注销人员删除失败");
                        for (OraclePersonInfo person : deletedPersons) {
                            result.addFailedRecord(person.getEmployeeNo(), person.getName(),
                                    "DELETE", "删除失败");
                        }
                    }
                } catch (Exception e) {
                    log.error("人员同步 - 注销人员删除异常: {}", e.getMessage(), e);
                    for (OraclePersonInfo person : deletedPersons) {
                        result.addFailedRecord(person.getEmployeeNo(), person.getName(),
                                "DELETE", "删除异常: " + e.getMessage());
                    }
                }
            }

            // 5. 转换并同步正常人员的基本信息
            List<WellPersonRequest> personRequests = new ArrayList<>();
            List<WellFaceRequest> faceRequests = new ArrayList<>();

            for (OraclePersonInfo person : activePersons) {
                // 转换人员请求
                WellPersonRequest personRequest = convertToWellPersonRequest(person);
                personRequests.add(personRequest);

                // 转换人脸请求（如果有照片）
                if (StringUtils.hasText(person.getPhotoBase64())) {
                    WellFaceRequest faceRequest = new WellFaceRequest();
                    faceRequest.setUserNo(person.getEmployeeNo());
                    faceRequest.setPhotoCodeStr(person.getPhotoBase64());
                    faceRequests.add(faceRequest);
                }
            }

            // 6. 批量同步人员信息
            if (!personRequests.isEmpty()) {
                log.info("人员同步 - 开始同步 {} 条人员基本信息", personRequests.size());
                try {
                    boolean personSuccess = wellPersonService.batchInsertOrUpdatePerson(personRequests);
                    if (personSuccess) {
                        result.setSuccess(result.getSuccess() + activePersons.size());
                        log.info("人员同步 - 人员基本信息同步成功");
                    } else {
                        result.setFailed(result.getFailed() + activePersons.size());
                        log.error("人员同步 - 人员基本信息批量同步失败");
                        for (OraclePersonInfo person : activePersons) {
                            result.addFailedRecord(person.getEmployeeNo(), person.getName(),
                                    "INSERT/UPDATE", "批量同步失败");
                        }
                        return result;
                    }
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + activePersons.size());
                    log.error("人员同步 - 人员基本信息同步异常: {}", e.getMessage(), e);
                    for (OraclePersonInfo person : activePersons) {
                        result.addFailedRecord(person.getEmployeeNo(), person.getName(),
                                "INSERT/UPDATE", "同步异常: " + e.getMessage());
                    }
                    return result;
                }
            }

            // 7. 批量同步人脸照片（包含人员数据中的照片）
            if (!faceRequests.isEmpty()) {
                log.info("人员同步 - 开始同步 {} 条人脸照片", faceRequests.size());
                try {
                    boolean faceSuccess = wellPersonService.batchInsertFace(faceRequests);
                    if (faceSuccess) {
                        result.setFaceSuccess(faceRequests.size());
                        log.info("人员同步 - 人脸照片同步成功");
                    } else {
                        result.setFaceFailed(faceRequests.size());
                        log.error("人员同步 - 人脸照片批量同步失败");
                    }
                } catch (Exception e) {
                    result.setFaceFailed(faceRequests.size());
                    log.error("人员同步 - 人脸照片同步异常: {}", e.getMessage(), e);
                }
            }

            // 8. 检查照片增量更新（处理只有照片变化但人员信息没变化的情况）
            syncUpdatedPhotos(lastSyncTime, result);

            // 9. 同步门禁授权（只同步正常人员）
            syncPersonGrants(activePersons, result);

        } catch (Exception e) {
            log.error("人员同步异常", e);
            result.addFailedRecord("", "", "SYNC", e.getMessage());
        }

        log.info("人员同步完成 - 总数:{}, 成功:{}, 失败:{}, 人脸成功:{}, 授权成功:{}",
                result.getTotal(), result.getSuccess(), result.getFailed(),
                result.getFaceSuccess(), result.getGrantSuccess());
        return result;
    }

    /**
     * 同步照片增量更新
     * 处理只有照片变化但人员信息没变化的情况
     * 
     * 照片表结构：
     *   - docu.photo: BXH(保险号), PHOTO_BF(照片), EDIT_DATETIME(编辑时间) - rylx=1
     *   - pentranceguard.tcfacephoto: SFZH(身份证号), PHOTO(照片), EDIT_DATETIME(编辑时间) - rylx=2,3
     *   - pentranceguard.personfacepicinfo: JLH(记录号), PHOTO(照片), EDIT_DATETIME(编辑时间) - rylx=4,5
     */
    private void syncUpdatedPhotos(LocalDateTime lastSyncTime, PersonSyncResult result) {
        log.info(">>> 开始检查照片增量更新");
        
        try {
            // 获取照片有更新的人员列表
            List<OracleDataService.PhotoUpdateInfo> updatedPhotos = 
                    oracleDataService.getUpdatedPhotoPersonIds(lastSyncTime);
            
            if (updatedPhotos.isEmpty()) {
                log.info("照片增量更新 - 无照片更新");
                return;
            }
            
            log.info("照片增量更新 - 发现 {} 条照片更新", updatedPhotos.size());
            
            // 构建人脸请求列表
            List<WellFaceRequest> faceRequests = new ArrayList<>();
            
            for (OracleDataService.PhotoUpdateInfo updateInfo : updatedPhotos) {
                String photoBase64 = oracleDataService.getPersonPhoto(
                        updateInfo.getPersonId(), updateInfo.getRylx());
                
                if (StringUtils.hasText(photoBase64)) {
                    WellFaceRequest faceRequest = new WellFaceRequest();
                    faceRequest.setUserNo(updateInfo.getPersonId());
                    faceRequest.setPhotoCodeStr(photoBase64);
                    faceRequests.add(faceRequest);
                    log.debug("照片增量更新 - 添加人员[{}]照片", updateInfo.getPersonId());
                }
            }
            
            if (faceRequests.isEmpty()) {
                log.info("照片增量更新 - 无有效照片需要同步");
                return;
            }
            
            // 批量同步照片
            log.info("照片增量更新 - 开始同步 {} 条照片", faceRequests.size());
            boolean success = wellPersonService.batchInsertFace(faceRequests);
            if (success) {
                result.setFaceSuccess(result.getFaceSuccess() + faceRequests.size());
                log.info("照片增量更新 - 同步成功");
            } else {
                result.setFaceFailed(result.getFaceFailed() + faceRequests.size());
                log.warn("照片增量更新 - 同步失败");
            }
            
        } catch (Exception e) {
            log.error("照片增量更新异常: {}", e.getMessage(), e);
        }
    }


    /**
     * 同步人员门禁授权
     * 统一使用临时时段授权（open-gating-single-grant接口）
     * 适用于所有人员类型：正式职工、长期外用工、施工人员、外来人员、子女工
     *
     * 门禁权限来源：
     * - 从Oracle查询到的CQDM字段（厂区代码）
     * - 通过GateCodeMappingUtil将CQDM映射到大门全称
     * - 调用威尔门禁列表接口，匹配placeName获取doorId
     *
     * Requirements: 3.4, 3.5
     */
    private void syncPersonGrants(List<OraclePersonInfo> persons, PersonSyncResult result) {
        log.info("开始同步人员门禁授权，人数: {}", persons.size());

        // 1. 获取威尔系统的门禁列表（缓存，避免重复调用）
        List<WellPersonService.DoorInfo> wellDoorList = wellPersonService.getDoorList();
        if (wellDoorList.isEmpty()) {
            log.warn("威尔门禁列表为空，使用默认配置");
        } else {
            log.info("获取到威尔门禁列表，共 {} 个门禁", wellDoorList.size());
        }

        // 2. 解析默认门禁ID列表（作为备用）
        List<Integer> defaultDoorIds = parseDoorIds(huagongXimenDoorIds);

        // 3. 为每个人员解析门禁权限
        for (OraclePersonInfo person : persons) {
            List<Integer> doorIds = new ArrayList<>();

            // 优先使用Oracle中的门禁权限（gatePermissionStr字段，包含聚合的CQDM）
            String gatePermissionStr = person.getGatePermissionStr();

            if (gatePermissionStr != null && !gatePermissionStr.trim().isEmpty()) {
                // 解析CQDM列表
                String[] cqdmArray = gatePermissionStr.split(",");

                for (String cqdm : cqdmArray) {
                    cqdm = cqdm.trim();
                    if (cqdm.isEmpty()) continue;

                    // 通过CQDM获取大门全称
                    String gateName = com.parkingmanage.util.GateCodeMappingUtil.getGateNameByCqdm(cqdm);

                    if (gateName != null) {
                        // 在威尔门禁列表中查找匹配的doorId
                        List<Integer> matchedDoorIds = wellPersonService.findDoorIdsByGateName(gateName, wellDoorList);

                        if (!matchedDoorIds.isEmpty()) {
                            // 添加匹配到的doorId（去重）
                            for (Integer doorId : matchedDoorIds) {
                                if (!doorIds.contains(doorId)) {
                                    doorIds.add(doorId);
                                }
                            }
                            // log.debug("人员[{}] CQDM={} -> 大门={} -> doorIds={}",
                            //         person.getEmployeeNo(), cqdm, gateName, matchedDoorIds);
                        } else {
                            // log.warn("人员[{}] CQDM={} -> 大门={} 在威尔系统中未找到匹配的门禁",
                            //         person.getEmployeeNo(), cqdm, gateName);
                        }
                    } else {
                        // log.warn("人员[{}] CQDM={} 未找到对应的大门名称", person.getEmployeeNo(), cqdm);
                    }
                }
            }

            // 如果没有匹配到任何门禁，使用默认配置
            if (doorIds.isEmpty()) {
                doorIds = defaultDoorIds;
                log.debug("人员[{}]使用默认门禁权限: {}", person.getEmployeeNo(), doorIds);
            } else {
                log.info("人员[{}]门禁权限: CQDM={} -> doorIds={}",
                        person.getEmployeeNo(), gatePermissionStr, doorIds);
            }

            person.setGatePermissions(doorIds);
        }

        // 4. 统一使用临时时段授权处理所有人员
        syncTemporaryPersonGrants(persons, result);
    }
    
    /**
     * 解析门禁ID字符串为列表
     * 
     * @param doorIdsStr 逗号分隔的门禁ID字符串
     * @return 门禁ID列表
     */
    private List<Integer> parseDoorIds(String doorIdsStr) {
        List<Integer> doorIds = new ArrayList<>();
        if (doorIdsStr == null || doorIdsStr.trim().isEmpty()) {
            return doorIds;
        }
        
        String[] ids = doorIdsStr.split(",");
        for (String id : ids) {
            try {
                doorIds.add(Integer.parseInt(id.trim()));
            } catch (NumberFormatException e) {
                log.warn("门禁ID格式错误: {}", id);
            }
        }
        
        return doorIds;
    }

    /**
     * 同步临时员工门禁授权（使用临时时段授权）
     * 调用接口：/api-gating/api-gating/open-gating-single-grant/batch/insert-or-update
     *
     * 适用于所有人员类型：正式职工、长期外用工、施工人员、外来人员、子女工
     * 每个人员有独立的授权时间段
     */
    private void syncTemporaryPersonGrants(List<OraclePersonInfo> persons, PersonSyncResult result) {
        log.info(">>> 开始同步人员门禁授权（临时时段），人数: {}", persons.size());

        List<WellSingleGrantRequest> singleGrantRequests = new ArrayList<>();
        int skippedNoTimeCount = 0;

        for (OraclePersonInfo person : persons) {
            // 检查时间是否完整，任一为空则跳过该人员的授权
            LocalDateTime beginTime = person.getValidStartTime();
            LocalDateTime endTime = person.getValidEndTime();

            if (beginTime == null || endTime == null) {
                skippedNoTimeCount++;
                log.debug("人员[{}]时间不完整（开始时间={}, 结束时间={}），跳过授权",
                        person.getEmployeeNo(), beginTime, endTime);
                continue;
            }

            // 格式化时间
            String beginTimeStr = beginTime.format(DATE_TIME_FORMATTER);
            String endTimeStr = endTime.format(DATE_TIME_FORMATTER);

            // 为每个门创建临时授权请求
            for (Integer doorId : person.getGatePermissions()) {
                WellSingleGrantRequest singleGrant = new WellSingleGrantRequest();
                singleGrant.setUserNo(person.getEmployeeNo());
                singleGrant.setDoorId(doorId);
                singleGrant.setEffectWay(1);      // 正向
                singleGrant.setTimeModel(1);      // 连续时段
                singleGrant.setBeginTime(beginTimeStr);
                singleGrant.setEndTime(endTimeStr);
                singleGrant.setSourceNo(person.getEmployeeNo() + "_" + doorId + "_single");
                singleGrantRequests.add(singleGrant);
            }
        }

        if (skippedNoTimeCount > 0) {
            log.info("人员门禁授权 - 跳过 {} 个时间不完整的人员", skippedNoTimeCount);
        }

        if (singleGrantRequests.isEmpty()) {
            log.info("人员门禁授权 - 无门禁授权需要同步");
            return;
        }
        log.info("人员门禁授权 - 开始同步 {} 条临时门禁授权", singleGrantRequests.size());
        boolean grantSuccess = wellPersonService.batchInsertOrUpdateSingleGrant(singleGrantRequests);
        if (grantSuccess) {
            result.setGrantSuccess(result.getGrantSuccess() + singleGrantRequests.size());
            log.info("人员门禁授权 - 门禁授权同步成功");
        } else {
            result.setGrantFailed(result.getGrantFailed() + singleGrantRequests.size());
            log.warn("人员门禁授权 - 门禁授权同步失败");
        }
    }

    /**
     * 转换Oracle人员信息为威尔人员请求
     */
    private WellPersonRequest convertToWellPersonRequest(OraclePersonInfo person) {
        WellPersonRequest request = new WellPersonRequest();
        
        // 根据人员类型名称(RYLXNAME)匹配威尔系统的部门编号
        String ptSourceNo = mapPersonTypeToDeptNo(person.getPersonType());
        request.setPtSourceNo(ptSourceNo);
        
        request.setUserName(person.getName());
        request.setSourceNo(person.getEmployeeNo());
        request.setUserType(6); // 职工
        request.setUserState(1); // 正常状态
        request.setUserSex(person.getSex() != null ? person.getSex() : 0);
        request.setUserIdentity(person.getIdCard());
        request.setPhoneNo(person.getPhone());
        request.setRemark(person.getRemark());
        return request;
    }

    /**
     * 根据人员类型名称映射到威尔系统的部门编号
     * 
     * 映射规则:
     *   正式员工/正式职工 → 151916
     *   子女工 → 151920
     *   外来员工/外来人员 → 151918
     *   长期外用工 → 151919
     *   施工人员 → 151917
     *   其他 → 11 (大庆石化公司)
     * 
     * @param personType 人员类型名称(RYLXNAME)
     * @return 威尔系统部门编号
     */
    private String mapPersonTypeToDeptNo(String personType) {
        if (personType == null || personType.trim().isEmpty()) {
            log.warn("人员类型为空，使用默认部门编号: 11");
            return "11";
        }
        String type = personType.trim();
        // 正式员工/正式职工
        if (type.contains("正式员工") || type.contains("正式职工")) {
            return "151916";
        }
        // 子女工
        else if (type.contains("子女工")) {
            return "151920";
        }
        // 外来员工/外来人员
        else if (type.contains("外来员工") || type.contains("外来人员")) {
            return "151918";
        }
        // 长期外用工
        else if (type.contains("长期外用工") || type.contains("外用工")) {
            return "151919";
        }
        // 施工人员
        else if (type.contains("施工人员") || type.contains("施工")) {
            return "151917";
        }
        // 其他类型，使用根部门
        else {
            log.debug("未匹配的人员类型: {}, 使用根部门编号: 11", type);
            return "11";
        }
    }


    @Override
    public VehicleSyncResult syncVehicleData() {
        VehicleSyncResult result = new VehicleSyncResult();
        try {
            // 1. 获取上次同步时间
            LocalDateTime lastSyncTime = getLastSyncTime();
            log.info("车辆同步 - 上次同步时间: {}", lastSyncTime);
            
            // 2. 从Oracle获取最新车辆数据（已按CZSJ排序）
            List<OracleVehicleInfo> vehicles = oracleDataService.getLatestVehicleData(lastSyncTime);
            if (vehicles.isEmpty()) {
                log.info("车辆同步 - 无新增或修改的车辆数据");
                return result;
            }
            log.info("车辆同步 - 获取到 {} 条车辆原始记录", vehicles.size());
            
            // 3. 按车牌号（CPH）分组
            // 同一车牌可能有多条记录（不同CQDM），需要合并处理
            List<GroupedVehicleInfo> groupedVehicles = VehicleGroupingUtil.groupByPlateNumber(vehicles);
            result.setTotal(groupedVehicles.size());
            
            log.info("车辆同步 - 分组后车辆数: {}", groupedVehicles.size());
            log.info("车辆同步 - 分组统计: {}", VehicleGroupingUtil.getGroupingStatistics(groupedVehicles));
            
            // 4. 逐个处理分组后的车辆数据
            for (GroupedVehicleInfo groupedVehicle : groupedVehicles) {
                try {
                    boolean vehicleSuccess = processGroupedVehicle(groupedVehicle, result);
                    if (vehicleSuccess) {
                        result.setSuccess(result.getSuccess() + 1);
                    } else {
                        result.setFailed(result.getFailed() + 1);
                    }
                } catch (Exception e) {
                    log.error("处理车辆[{}]异常: {}", groupedVehicle.getPlateNumber(), e.getMessage());
                    result.setFailed(result.getFailed() + 1);
                    result.addFailedRecord(groupedVehicle.getPlateNumber(), groupedVehicle.getOwnerName(), 
                            "PROCESS", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("车辆同步异常", e);
            result.addFailedRecord("", "", "SYNC", e.getMessage());
        }
        log.info("车辆同步完成 - 总数:{}, 成功:{}, 失败:{}, VIP开通:{}, 黑名单:{}", 
                result.getTotal(), result.getSuccess(), result.getFailed(),
                result.getVipOpenSuccess(), result.getBlacklistSuccess());
        return result;
    }

    /**
     * 处理分组后的车辆数据
     *
     * 这是新的处理方法，接收GroupedVehicleInfo对象
     * 同一车牌的多条记录已经合并，orgNames包含所有厂区名称
     *
     * 处理逻辑（按优先级）：
     * 1. 优先检查 DQZT 状态：
     *    - DQZT=D（注销）：查询AKE系统有什么数据（VIP或黑名单），有什么就删除什么
     *    - DQZT=A（正常）：根据 ISCHECK 判断走 VIP 还是黑名单流程
     * 2. 正常状态时根据 ISCHECK 分流：
     *    - ISCHECK=1（是）：黑名单流程
     *    - ISCHECK=0（否）：VIP流程
     *
     * Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2
     */
    private boolean processGroupedVehicle(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("处理分组车辆: {}, 车主: {}, 记录数: {}, 厂区: {}, DQZT: {}, ISCHECK: {}",
                plateNumber, groupedVehicle.getOwnerName(),
                groupedVehicle.getRecordCount(), groupedVehicle.getOrgNames(),
                groupedVehicle.getRemark(), groupedVehicle.isNeedCheck() ? "是" : "否");

        if (groupedVehicle.getOriginalRecords().isEmpty()) {
            log.warn("车辆[{}]无原始记录，跳过处理", plateNumber);
            return false;
        }

        // 步骤1：优先检查 DQZT 状态（注销状态优先处理）
        // dqzt字段存储的是DQZT（当前状态代码），"D"表示注销状态，"A"表示正常状态
        if ("D".equals(groupedVehicle.getDqzt())) {
            log.info("车辆[{}]状态为注销(D)，执行注销处理流程", plateNumber);
            return processDeletionFlow(groupedVehicle, result);
        }

        // 步骤2：正常状态，检查 KLX 卡类型
        String klx = groupedVehicle.getKlx();
        log.info("车辆[{}]状态正常(A)，卡类型: {}", plateNumber, klx);

        if ("D".equals(klx)) {
            // KLX=D（临时卡）：需要同时满足两个条件才添加访客
            // 1. ISCHECK=1（需要检查）
            // 2. 厂区代码为支持的访客门：0301(化工西门)、0201(化肥西门)、0501(复合肥南门)
            if (groupedVehicle.isNeedCheck()) {
                // ISCHECK=1，检查厂区代码是否为支持的访客门
                String visitName = determineVisitorNameByOrgNo(groupedVehicle.getOrgNos());
                if (visitName != null) {
                    // 厂区代码是支持的访客门，添加访客
                    log.info("车辆[{}]临时卡需要检查(ISCHECK=1)，厂区代码={}，进入访客同步流程: {}",
                            plateNumber, groupedVehicle.getOrgNos(), visitName);
                    return processVisitorSync(groupedVehicle, visitName, result);
                } else {
                    // 厂区代码不是支持的访客门，跳过处理
                    log.info("车辆[{}]临时卡需要检查(ISCHECK=1)，但厂区代码={}不在访客支持范围内，跳过处理",
                            plateNumber, groupedVehicle.getOrgNos());
                    return true; // 返回true表示成功跳过
                }
            } else {
                // ISCHECK=0，添加到黑名单
                log.info("车辆[{}]临时卡不需检查(ISCHECK=0)，进入黑名单同步流程", plateNumber);
                return processBlacklistSync(groupedVehicle, result);
            }
        } else {
            // KLX=A（长期卡）：检查 ISCHECK
            if (groupedVehicle.isNeedCheck()) {
                // ISCHECK=1：原来VIP类型流程（请停车检查格式）
                log.info("车辆[{}]长期卡需要检查(ISCHECK=1)，进入VIP同步流程", plateNumber);
                return processVipSync(groupedVehicle, result);
            } else {
                // ISCHECK=0：黑名单流程
                log.info("车辆[{}]长期卡不需检查(ISCHECK=0)，进入黑名单同步流程", plateNumber);
                return processBlacklistSync(groupedVehicle, result);
            }
        }
    }

    /**
     * 注销处理流程
     *
     * 当 DQZT=D（注销）时，不管 ISCHECK 是什么，都应该：
     * 1. 查询AKE系统中该车辆有什么数据（VIP或黑名单）
     * 2. 有什么就删除/退费什么
     * 3. 不会创建新的VIP或黑名单数据
     *
     * 处理的数据类型包括：
     * - VIP月票（请停车检查（厂区）格式）
     * - 临时来访VIP（临时来访（化工西门）、临时来访（化肥西门）、临时来访（复合肥南门））
     * - 黑名单（厂区VIP格式）
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param result 同步结果
     * @return 处理是否成功
     */
    private boolean processDeletionFlow(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]开始注销处理", plateNumber);

        boolean allSuccess = true;
        int deletedCount = 0;

        // 1. 检查并删除/退费VIP
        List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
        if (existingVips != null && !existingVips.isEmpty()) {
            log.info("车辆[{}]在AKE系统中有 {} 条VIP记录，执行退费操作", plateNumber, existingVips.size());

            // 筛选生效中的VIP票
            for (AkeVipService.VipTicketInfo vip : existingVips) {
                if ("生效中".equals(vip.getTicketStatus())) {
                    boolean refundSuccess = akeVipService.refundVipTicket(
                            vip.getVipTicketSeq(), null, null, "0");
                    if (refundSuccess) {
                        result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                        deletedCount++;
                        log.info("车辆[{}]VIP退费成功: {}", plateNumber, vip.getVipTypeName());
                    } else {
                        result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                        result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                                "DELETE_VIP", "VIP退费失败: " + vip.getVipTypeName());
                        allSuccess = false;
                        log.warn("车辆[{}]VIP退费失败: {}", plateNumber, vip.getVipTypeName());
                    }
                } else {
                    log.debug("车辆[{}]VIP状态为[{}]，跳过退费", plateNumber, vip.getTicketStatus());
                }
            }
        } else {
            log.debug("车辆[{}]在AKE系统中无VIP记录", plateNumber);
        }

        // 2. 检查并删除黑名单
        AkeVipService.BlacklistInfo existingBlacklist = akeVipService.getBlacklistByPlateNumber(plateNumber);
        if (existingBlacklist != null) {
            log.info("车辆[{}]在AKE系统中有黑名单记录，执行删除操作", plateNumber);

            boolean deleteSuccess = akeVipService.deleteBlacklistCar(
                    existingBlacklist.getBlacklistSeq(), null, null);

            if (deleteSuccess) {
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                deletedCount++;
                log.info("车辆[{}]黑名单删除成功: {}", plateNumber, existingBlacklist.getVipName());
            } else {
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "DELETE_BLACKLIST", "黑名单删除失败");
                allSuccess = false;
                log.warn("车辆[{}]黑名单删除失败", plateNumber);
            }
        } else {
            log.debug("车辆[{}]在AKE系统中无黑名单记录", plateNumber);
        }

        if (deletedCount == 0) {
            log.info("车辆[{}]在AKE系统中无任何需要删除的数据（VIP/黑名单）", plateNumber);
        } else {
            log.info("车辆[{}]注销处理完成，共删除 {} 条数据", plateNumber, deletedCount);
        }

        return allSuccess;
    }
    
    /**
     * VIP月票同步流程
     * 
     * 流程：
     * 1. 检查DQZT状态，如果是"D"则退费并结束
     * 2. 查询ake系统现有VIP信息（筛选生效中的）
     * 3. 如果无VIP：合并权限并开通
     * 4. 如果有VIP：比较权限
     *    - 权限不同：先退费再开通
     *    - 权限相同：比较时间，决定是否续费
     * 
     * Requirements: 5.2, 5.3, 5.4, 5.5
     */
    private boolean processVipSync(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        
        // 步骤1：检查DQZT状态
        // remark字段存储的是DQZTNAME（当前状态名称）
        // 如果是"D"表示注销状态，需要退费
        if ("D".equals(groupedVehicle.getRemark())) {
            log.info("车辆[{}]状态为注销(D)，执行退费操作", plateNumber);
            boolean refundSuccess = akeVipService.refundVipTicketByPlateNumber(plateNumber);
            if (refundSuccess) {
                result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                log.info("车辆[{}]退费成功", plateNumber);
            } else {
                result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(), 
                        "VIP_REFUND", "注销状态退费失败");
                log.warn("车辆[{}]退费失败", plateNumber);
            }
            // 注销状态处理完退费后结束
            return refundSuccess;
        }
        
        // 步骤2：查询ake系统现有VIP信息
        List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);
        
        // 筛选生效中的VIP票
        List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
        if (existingVips != null) {
            for (AkeVipService.VipTicketInfo vip : existingVips) {
                if ("生效中".equals(vip.getTicketStatus())) {
                    activeVips.add(vip);
                }
            }
        }
        
        log.info("车辆[{}]在ake系统中有 {} 条生效中的VIP票", plateNumber, activeVips.size());
        
        // 步骤3和4：根据是否有现有VIP决定处理逻辑
        if (activeVips.isEmpty()) {
            // 情况A：无现有VIP，直接开通
            return openNewVip(groupedVehicle, result);
        } else {
            // 情况B：有现有VIP，需要比较权限和时间
            return updateExistingVip(groupedVehicle, activeVips, result);
        }
    }
    
    /**
     * 开通新VIP（无现有VIP的情况）
     * 
     * 合并同车牌的多个CQDMNAME为一个vip_type_name
     * 调用OPEN_VIP_TICKET接口（0元开通）
     * 只调用一次接口
     * 
     * Requirements: 5.4
     */
    private boolean openNewVip(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]无现有VIP，开通新VIP", plateNumber);

        try {
            // 构建开通请求（使用分组后的车辆信息）
            OpenVipTicketRequest request = buildOpenVipTicketRequestFromGrouped(groupedVehicle);

            if (request == null) {
                result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "VIP_OPEN", "无法生成有效的VIP类型");
                log.warn("车辆[{}]无法生成有效的VIP类型", plateNumber);
                return false;
            }

            // 调用开通接口
            boolean success = akeVipService.openVipTicket(request);

            if (success) {
                result.setVipOpenSuccess(result.getVipOpenSuccess() + 1);
                log.info("车辆[{}]开通VIP成功，类型: {}", plateNumber, request.getVipTypeName());
                return true;
            } else {
                result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "VIP_OPEN", "开通VIP失败");
                log.warn("车辆[{}]开通VIP失败", plateNumber);
                return false;
            }
        } catch (Exception e) {
            log.error("车辆[{}]开通VIP异常: {}", plateNumber, e.getMessage(), e);
            result.setVipOpenFailed(result.getVipOpenFailed() + 1);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                    "VIP_OPEN", e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新现有VIP（有现有VIP的情况）
     * 
     * 比较Oracle权限集合和ake权限集合
     * - 权限不同：先退费再开通
     * - 权限相同：比较时间，决定是否续费
     * 
     * Requirements: 5.2, 5.3, 5.4, 5.5
     */
    private boolean updateExistingVip(GroupedVehicleInfo groupedVehicle, 
                                      List<AkeVipService.VipTicketInfo> activeVips,
                                      VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]有现有VIP，检查是否需要更新", plateNumber);
        
        // 取第一条生效中的VIP（理论上一个车牌只应该有一条VIP）
        AkeVipService.VipTicketInfo existingVip = activeVips.get(0);
        if (activeVips.size() > 1) {
            log.warn("车辆[{}]有多条生效中的VIP票({}条)，使用第一条进行比较", 
                    plateNumber, activeVips.size());
        }
        
        // 提取Oracle权限集合（从orgNames转换为ake简化名称）
        Set<String> oraclePermissions = VipPermissionUtil.extractPermissionsFromOracleGateNames(
                groupedVehicle.getOrgNames());
        log.info("车辆[{}]Oracle权限: {}", plateNumber, oraclePermissions);
        
        // 提取ake权限集合（从vip_type_name中解析）
        // 注意：现在VIP类型名称格式是"请停车检查（xxx）"，使用extractPermissionsFromBlacklistType提取
        Set<String> akePermissions = VipPermissionUtil.extractPermissionsFromBlacklistType(
                existingVip.getVipTypeName());
        log.info("车辆[{}]ake权限: {}", plateNumber, akePermissions);
        
        // 比较权限
        boolean permissionsEqual = VipPermissionUtil.arePermissionsEqual(oraclePermissions, akePermissions);
        
        if (!permissionsEqual) {
            // 权限不同：先退费再开通
            log.info("车辆[{}]权限变化，执行退费+开通流程", plateNumber);
            
            try {
                // 先退费
                boolean refundSuccess = akeVipService.refundVipTicket(
                        existingVip.getVipTicketSeq(), null, null, "0");
                
                if (refundSuccess) {
                    result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                    log.info("车辆[{}]退费成功", plateNumber);
                } else {
                    result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                    log.warn("车辆[{}]退费失败，继续尝试开通", plateNumber);
                }
                
                // 再开通新VIP（使用新权限）
                return openNewVip(groupedVehicle, result);
                
            } catch (Exception e) {
                log.error("车辆[{}]权限变化处理异常: {}", plateNumber, e.getMessage(), e);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(), 
                        "VIP_UPDATE", e.getMessage());
                return false;
            }
        } else {
            // 权限相同：比较时间
            log.info("车辆[{}]权限相同，检查时间是否需要更新", plateNumber);
            return checkAndUpdateVipTime(groupedVehicle, existingVip, result);
        }
    }
    
    /**
     * 检查并更新VIP时间（权限相同时）
     *
     * 比较ake系统中的time_period和Oracle的KYXQKSSJ、KYXQJSSJ
     * - AKE时间范围 > Oracle时间范围：先退费再开通
     * - AKE时间范围 = Oracle时间范围：不操作
     * - AKE时间范围 < Oracle时间范围：续费
     *
     * Requirements: 5.5
     */
    private boolean checkAndUpdateVipTime(GroupedVehicleInfo groupedVehicle,
                                         AkeVipService.VipTicketInfo existingVip,
                                         VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();

        try {
            // 获取Oracle的时间
            LocalDateTime oracleStartTime = groupedVehicle.getValidStartTime();
            LocalDateTime oracleEndTime = groupedVehicle.getValidEndTime();

            if (oracleStartTime == null || oracleEndTime == null) {
                log.warn("车辆[{}]Oracle时间为空，跳过时间更新", plateNumber);
                return true;
            }

            // 获取ake系统的时间
            String akeStartTime = existingVip.getStartTime();
            String akeEndTime = existingVip.getEndTime();

            if (akeStartTime == null || akeEndTime == null) {
                log.warn("车辆[{}]ake时间为空，跳过时间更新", plateNumber);
                return true;
            }

            // 格式化Oracle时间为字符串（用于比较）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String oracleStartStr = oracleStartTime.format(formatter);
            String oracleEndStr = oracleEndTime.format(formatter);

            log.info("车辆[{}]时间比较 - Oracle: {} ~ {}, ake: {} ~ {}",
                    plateNumber, oracleStartStr, oracleEndStr, akeStartTime, akeEndTime);

            // 比较时间（允许秒级误差）
            boolean startTimeEqual = isTimeEqual(oracleStartStr, akeStartTime);
            boolean endTimeEqual = isTimeEqual(oracleEndStr, akeEndTime);

            // 比较结束时间的大小关系
            int endTimeCompare = compareTime(oracleEndStr, akeEndTime);

            if (startTimeEqual && endTimeEqual) {
                // 时间完全相同，不操作
                log.info("车辆[{}]时间相同，无需更新", plateNumber);
                return true;
            } else if (endTimeCompare < 0) {
                // AKE结束时间 > Oracle结束时间，先退费再开通
                log.info("车辆[{}]AKE时间范围更大(ACE结束时间 > Oracle结束时间)，执行退费+开通流程", plateNumber);

                // 先退费
                boolean refundSuccess = akeVipService.refundVipTicket(
                        existingVip.getVipTicketSeq(), null, null, "0");

                if (refundSuccess) {
                    result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                    log.info("车辆[{}]退费成功", plateNumber);
                } else {
                    result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                    log.warn("车辆[{}]退费失败，继续尝试开通", plateNumber);
                }

                // 再开通新VIP
                return openNewVip(groupedVehicle, result);
            } else {
                // AKE结束时间 < Oracle结束时间，续费
                log.info("车辆[{}]AKE时间范围更小(AKE结束时间 < Oracle结束时间)，执行续费操作", plateNumber);

                boolean renewSuccess = akeVipService.renewVipTicket(
                        existingVip.getVipTicketSeq(),
                        oracleStartStr,
                        oracleEndStr,
                        null,
                        null);

                if (renewSuccess) {
                    result.setVipRenewSuccess(result.getVipRenewSuccess() + 1);
                    log.info("车辆[{}]续费成功", plateNumber);
                    return true;
                } else {
                    result.setVipRenewFailed(result.getVipRenewFailed() + 1);
                    result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                            "VIP_RENEW", "续费失败");
                    log.warn("车辆[{}]续费失败", plateNumber);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("车辆[{}]时间更新异常: {}", plateNumber, e.getMessage(), e);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                    "VIP_TIME_UPDATE", e.getMessage());
            return false;
        }
    }
    
    /**
     * 比较两个时间字符串是否相等（允许秒级误差）
     *
     * @param time1 时间字符串1（格式：yyyy-MM-dd HH:mm:ss）
     * @param time2 时间字符串2（格式：yyyy-MM-dd HH:mm:ss）
     * @return true表示时间相同
     */
    private boolean isTimeEqual(String time1, String time2) {
        if (time1 == null && time2 == null) {
            return true;
        }
        if (time1 == null || time2 == null) {
            return false;
        }

        // 简单的字符串比较（精确到秒）
        return time1.trim().equals(time2.trim());
    }

    /**
     * 比较两个时间字符串的大小
     *
     * @param time1 时间字符串1（格式：yyyy-MM-dd HH:mm:ss）
     * @param time2 时间字符串2（格式：yyyy-MM-dd HH:mm:ss）
     * @return 负数表示 time1 < time2，0表示相等，正数表示 time1 > time2
     */
    private int compareTime(String time1, String time2) {
        if (time1 == null && time2 == null) {
            return 0;
        }
        if (time1 == null) {
            return -1;
        }
        if (time2 == null) {
            return 1;
        }

        // 字符串比较（格式统一，可以直接比较）
        return time1.trim().compareTo(time2.trim());
    }

    /**
     * 临时来访VIP同步流程
     *
     * 用于处理 KLX=D（临时卡）且 CQDM 为支持的临时来访VIP门的车辆
     *
     * VIP类型固定为三种：
     * - 临时来访（化工西门）- CQDM: 0301
     * - 临时来访（化肥西门）- CQDM: 0201
     * - 临时来访（复合肥南门）- CQDM: 0501
     *
     * 流程：
     * 1. 使用传入的临时来访VIP类型（已根据CQDM确定）
     * 2. 查询ake系统现有VIP信息（筛选生效中的）
     * 3. 如果无VIP：直接开通对应的临时来访VIP
     * 4. 如果有VIP：比较VIP类型
     *    - 类型不同：先退费再开通
     *    - 类型相同：比较时间，决定是否续费
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param tempVisitVipType 临时来访VIP类型（已根据CQDM确定）
     * @param result 同步结果
     * @return 处理是否成功
     */
    private boolean processTempVisitVip(GroupedVehicleInfo groupedVehicle, String tempVisitVipType, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]进入临时来访VIP同步流程，类型: {}", plateNumber, tempVisitVipType);

        // 步骤2：查询ake系统现有VIP信息
        List<AkeVipService.VipTicketInfo> existingVips = akeVipService.getVipTicket(plateNumber, null, null);

        // 筛选生效中的VIP票
        List<AkeVipService.VipTicketInfo> activeVips = new ArrayList<>();
        if (existingVips != null) {
            for (AkeVipService.VipTicketInfo vip : existingVips) {
                if ("生效中".equals(vip.getTicketStatus())) {
                    activeVips.add(vip);
                }
            }
        }

        log.info("车辆[{}]在ake系统中有 {} 条生效中的VIP票", plateNumber, activeVips.size());

        // 步骤3和4：根据是否有现有VIP决定处理逻辑
        if (activeVips.isEmpty()) {
            // 情况A：无现有VIP，直接开通临时来访VIP
            return openNewTempVisitVip(groupedVehicle, tempVisitVipType, result);
        } else {
            // 情况B：有现有VIP，需要比较类型和时间
            return updateExistingTempVisitVip(groupedVehicle, tempVisitVipType, activeVips, result);
        }
    }

    /**
     * 访客车辆同步流程
     *
     * 用于处理 KLX=D（临时卡）且 ISCHECK=1 且 CQDM 为 0301/0201/0501 的车辆
     *
     * 访客类型：
     * - 0301 → 来访车辆（化工西门）
     * - 0201 → 来访车辆（化肥西门）
     * - 0501 → 来访车辆（复合肥南门）
     *
     * 流程：
     * 1. 根据厂区代码确定访客类型
     * 2. 构建访客请求（使用智能生成填充必填字段）
     * 3. 调用 ADD_VISITOR_CAR 接口
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param visitName 访客类型名称（已根据CQDM确定）
     * @param result 同步结果
     * @return 处理是否成功
     */
    private boolean processVisitorSync(GroupedVehicleInfo groupedVehicle, String visitName, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]进入访客同步流程，访客类型: {}", plateNumber, visitName);

        try {
            // 构建访客请求
            com.parkingmanage.dto.ake.AddVisitorCarRequest request = buildVisitorRequest(groupedVehicle, visitName);

            // 调用添加访客接口
            boolean success = akeVipService.addVisitorCar(request);

            if (success) {
                result.setVipOpenSuccess(result.getVipOpenSuccess() + 1);
                log.info("车辆[{}]访客添加成功: {}", plateNumber, visitName);
                return true;
            } else {
                result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "VISITOR_ADD", "访客添加失败");
                log.warn("车辆[{}]访客添加失败: {}", plateNumber, visitName);
                return false;
            }
        } catch (Exception e) {
            log.error("车辆[{}]添加访客异常: {}", plateNumber, e.getMessage(), e);
            result.setVipOpenFailed(result.getVipOpenFailed() + 1);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                    "VISITOR_ADD", e.getMessage());
            return false;
        }
    }

    /**
     * 构建访客车辆请求
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param visitName 访客类型名称
     * @return 访客请求
     */
    private com.parkingmanage.dto.ake.AddVisitorCarRequest buildVisitorRequest(GroupedVehicleInfo groupedVehicle,
                                                                              String visitName) {
        com.parkingmanage.dto.ake.AddVisitorCarRequest request = new com.parkingmanage.dto.ake.AddVisitorCarRequest();

        request.setCarCode(groupedVehicle.getPlateNumber());

        // 车主姓名：如果查询不到则使用车牌号码
        String owner = groupedVehicle.getOwnerName();
        if (owner == null || owner.trim().isEmpty()) {
            owner = groupedVehicle.getPlateNumber();
        }
        request.setOwner(owner);

        request.setVisitName(visitName);

        // 手机号：如果查询不到则使用时间戳生成唯一号码（格式：13+时间戳后9位）
        String phonenum = groupedVehicle.getOwnerPhone();
        if (phonenum == null || phonenum.trim().isEmpty()) {
            phonenum = "13" + String.valueOf(System.currentTimeMillis()).substring(4);
        }
        request.setPhonenum(phonenum);

        // 访问原因：根据访客类型生成
        String reason = "来访";
        if (visitName.contains("化工西门")) {
            reason = "化工西门来访";
        } else if (visitName.contains("化肥西门")) {
            reason = "化肥西门来访";
        } else if (visitName.contains("复合肥南门")) {
            reason = "复合肥南门来访";
        }
        request.setReason(reason);

        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());

        // 访问时间
        com.parkingmanage.dto.ake.AddVisitorCarRequest.VisitTime visitTime =
                new com.parkingmanage.dto.ake.AddVisitorCarRequest.VisitTime();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (groupedVehicle.getValidStartTime() != null) {
            visitTime.setStartTime(groupedVehicle.getValidStartTime().format(formatter));
        } else {
            visitTime.setStartTime(LocalDateTime.now().format(formatter));
        }

        if (groupedVehicle.getValidEndTime() != null) {
            visitTime.setEndTime(groupedVehicle.getValidEndTime().format(formatter));
        } else {
            // 默认当日结束
            visitTime.setEndTime(LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).format(formatter));
        }

        request.setVisitTime(visitTime);

        return request;
    }

    /**
     * 根据厂区代码列表确定访客类型名称
     *
     * 匹配规则（使用厂区代码CQDM）：
     * - 只处理以下三个访客类型：
     *   1. 0301 → 来访车辆（化工西门）
     *   2. 0201 → 来访车辆（化肥西门）
     *   3. 0501 → 来访车辆（复合肥南门）
     * - 如果厂区代码不在以上三个范围内，返回null（不支持访客）
     *
     * @param orgNos Oracle厂区代码列表（CQDM）
     * @return 访客类型名称，如果不支持则返回null
     */
    private String determineVisitorNameByOrgNo(List<String> orgNos) {
        if (orgNos == null || orgNos.isEmpty()) {
            return null;
        }

        // 按优先级检查厂区代码
        for (String orgNo : orgNos) {
            if (orgNo != null) {
                switch (orgNo.trim()) {
                    case "0301":
                        return "来访车辆（化工西门）";
                    case "0201":
                        return "来访车辆（化肥西门）";
                    case "0501":
                        return "来访车辆（复合肥南门）";
                    default:
                        // 其他厂区代码不处理访客
                        break;
                }
            }
        }

        // 没有匹配到访客支持的厂区代码
        return null;
    }

    /**
     * 根据厂区代码列表确定临时来访VIP类型
     *
     * 匹配规则（使用厂区代码CQDM）：
     * - 只处理以下三个临时来访VIP类型：
     *   1. 0301 → 临时来访（化工西门）
     *   2. 0201 → 临时来访（化肥西门）
     *   3. 0501 → 临时来访（复合肥南门）
     * - 如果厂区代码不在以上三个范围内，返回null（不支持临时来访VIP）
     *
     * @param orgNos Oracle厂区代码列表（CQDM）
     * @return 临时来访VIP类型名称，如果不支持则返回null
     */
    private String determineTempVisitVipTypeByOrgNo(List<String> orgNos) {
        if (orgNos == null || orgNos.isEmpty()) {
            return null;
        }

        // 按优先级检查厂区代码
        for (String orgNo : orgNos) {
            if (orgNo != null) {
                switch (orgNo.trim()) {
                    case "0301":
                        return "临时来访（化工西门）";
                    case "0201":
                        return "临时来访（化肥西门）";
                    case "0501":
                        return "临时来访（复合肥南门）";
                    default:
                        // 其他厂区代码不处理临时来访VIP
                        break;
                }
            }
        }

        // 没有匹配到临时来访VIP支持的厂区代码
        return null;
    }

    /**
     * 根据厂区名称列表确定临时来访VIP类型
     *
     * 匹配规则（使用智能匹配，只保留AKE系统中存在的门）：
     * - Oracle厂区名称通过GateNameMapper转换为AKE简化名称
     * - 只处理AKE系统支持的三个临时来访VIP类型：
     *   1. 化工西 → 临时来访（化工西门）
     *   2. 化肥西 → 临时来访（化肥西门）
     *   3. 复合肥南 → 临时来访（复合肥南门）
     * - 如果Oracle厂区名称无法映射到AKE系统支持的厂区，则跳过
     * - 默认：临时来访（化工西门）
     *
     * @param orgNames Oracle厂区名称列表
     * @return 临时来访VIP类型名称
     */
    private String determineTempVisitVipType(List<String> orgNames) {
        if (orgNames == null || orgNames.isEmpty()) {
            return "临时来访（化工西门）";
        }

        // 使用GateNameMapper进行智能匹配，只保留AKE系统中存在的门
        Set<String> akeGateNames = VipPermissionUtil.extractPermissionsFromOracleGateNames(orgNames);

        if (akeGateNames.isEmpty()) {
            // 没有匹配到AKE系统的门，返回默认值
            return "临时来访（化工西门）";
        }

        // 按优先级检查匹配到的AKE门名称
        for (String akeGateName : akeGateNames) {
            if (akeGateName != null) {
                switch (akeGateName) {
                    case "化工西":
                        return "临时来访（化工西门）";
                    case "化肥西":
                        return "临时来访（化肥西门）";
                    case "复合肥南":
                        return "临时来访（复合肥南门）";
                    default:
                        // 其他门（如炼油南、炼油西一等）不处理临时来访VIP
                        break;
                }
            }
        }

        // 没有匹配到临时来访VIP支持的厂区，返回默认值
        return "临时来访（化工西门）";
    }

    /**
     * 开通新的临时来访VIP（无现有VIP的情况）
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param tempVisitVipType 临时来访VIP类型名称
     * @param result 同步结果
     * @return 是否成功
     */
    private boolean openNewTempVisitVip(GroupedVehicleInfo groupedVehicle, String tempVisitVipType,
                                        VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]无现有VIP，开通新的临时来访VIP: {}", plateNumber, tempVisitVipType);

        try {
            // 构建开通VIP票请求（使用临时来访VIP类型）
            OpenVipTicketRequest request = buildTempVisitVipRequest(groupedVehicle, tempVisitVipType);

            // 调用开通接口
            boolean success = akeVipService.openVipTicket(request);

            if (success) {
                result.setVipOpenSuccess(result.getVipOpenSuccess() + 1);
                log.info("车辆[{}]临时来访VIP开通成功: {}", plateNumber, tempVisitVipType);
                return true;
            } else {
                result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "TEMP_VISIT_VIP_OPEN", "临时来访VIP开通失败");
                log.warn("车辆[{}]临时来访VIP开通失败: {}", plateNumber, tempVisitVipType);
                return false;
            }
        } catch (Exception e) {
            log.error("车辆[{}]开通临时来访VIP异常: {}", plateNumber, e.getMessage(), e);
            result.setVipOpenFailed(result.getVipOpenFailed() + 1);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                    "TEMP_VISIT_VIP_OPEN", e.getMessage());
            return false;
        }
    }

    /**
     * 更新现有临时来访VIP（有现有VIP的情况）
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param tempVisitVipType 临时来访VIP类型名称
     * @param activeVips 生效中的VIP列表
     * @param result 同步结果
     * @return 是否成功
     */
    private boolean updateExistingTempVisitVip(GroupedVehicleInfo groupedVehicle, String tempVisitVipType,
                                               List<AkeVipService.VipTicketInfo> activeVips,
                                               VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]有现有VIP，检查是否需要更新临时来访VIP", plateNumber);

        // 获取第一条现有VIP（同车牌应该只有一种类型的VIP）
        AkeVipService.VipTicketInfo existingVip = activeVips.get(0);
        String existingVipType = existingVip.getVipTypeName();

        // 比较VIP类型
        if (!tempVisitVipType.equals(existingVipType)) {
            // VIP类型不同，先退费再开通
            log.info("车辆[{}]VIP类型不同(现有: {}, 需要: {})，执行退费+开通流程",
                    plateNumber, existingVipType, tempVisitVipType);

            // 先退费
            boolean refundSuccess = akeVipService.refundVipTicket(
                    existingVip.getVipTicketSeq(), null, null, "0");

            if (refundSuccess) {
                result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                log.info("车辆[{}]退费成功", plateNumber);
            } else {
                result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                log.warn("车辆[{}]退费失败，继续尝试开通", plateNumber);
            }

            // 再开通新VIP
            return openNewTempVisitVip(groupedVehicle, tempVisitVipType, result);
        } else {
            // VIP类型相同，比较时间
            log.info("车辆[{}]VIP类型相同({})，比较时间决定是否续费", plateNumber, tempVisitVipType);
            return checkAndUpdateVipTime(groupedVehicle, existingVip, result);
        }
    }

    /**
     * 构建临时来访VIP开通请求
     *
     * @param groupedVehicle 分组后的车辆信息
     * @param tempVisitVipType 临时来访VIP类型名称
     * @return 开通VIP票请求
     */
    private OpenVipTicketRequest buildTempVisitVipRequest(GroupedVehicleInfo groupedVehicle,
                                                          String tempVisitVipType) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();

        request.setVipTypeName(tempVisitVipType);
        request.setTicketNo(generateTicketNo(groupedVehicle.getPlateNumber()));
        // 车主姓名：如果查询不到则使用车牌号码
        String carOwner = groupedVehicle.getOwnerName();
        if (carOwner == null || carOwner.trim().isEmpty()) {
            carOwner = groupedVehicle.getPlateNumber();
        }
        request.setCarOwner(carOwner);
        // 车主电话：如果查询不到则使用时间戳生成唯一号码（格式：13+时间戳后9位）
        String telphone = groupedVehicle.getOwnerPhone();
        if (telphone == null || telphone.trim().isEmpty()) {
            telphone = "13" + String.valueOf(System.currentTimeMillis()).substring(4);
        }
        request.setTelphone(telphone);
        request.setCompany(groupedVehicle.getCompany());
        request.setDepartment(groupedVehicle.getCompany());
        request.setSex("0");
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());
        request.setOriginalPrice("0");
        request.setDiscountPrice("0");
        request.setOpenValue("1");
        request.setOpenCarCount("1");

        // 车辆列表
        List<OpenVipTicketRequest.CarInfo> carList = new ArrayList<>();
        OpenVipTicketRequest.CarInfo carInfo = new OpenVipTicketRequest.CarInfo();
        carInfo.setCarNo(groupedVehicle.getPlateNumber());
        carList.add(carInfo);
        request.setCarList(carList);

        // 时间段列表（有效期）
        List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
        OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (groupedVehicle.getValidStartTime() != null) {
            timePeriod.setStartTime(groupedVehicle.getValidStartTime().format(formatter));
        } else {
            timePeriod.setStartTime(LocalDateTime.now().format(formatter));
        }

        if (groupedVehicle.getValidEndTime() != null) {
            timePeriod.setEndTime(groupedVehicle.getValidEndTime().format(formatter));
        } else {
            timePeriod.setEndTime(LocalDateTime.now().plusYears(1).format(formatter));
        }

        timePeriodList.add(timePeriod);
        request.setTimePeriodList(timePeriodList);

        return request;
    }

    /**
     * 黑名单同步流程
     * 
     * 流程：
     * 1. 检查DQZT状态，如果是"D"则删除黑名单并结束
     * 2. 查询ake系统现有黑名单信息（获取所有黑名单后筛选）
     * 3. 如果无黑名单：合并权限并添加
     * 4. 如果有黑名单：比较权限
     *    - 权限不同：先删除再添加
     *    - 权限相同：检查永久标志
     *      - 永久（blacklist_forever_flag="1"）：跳过
     *      - 临时（blacklist_forever_flag="0"）：比较时间，决定是否更新
     * 
     * Requirements: 6.1, 6.2, 6.3
     */
    private boolean processBlacklistSync(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]进入黑名单同步流程", plateNumber);
        
        // 步骤1：检查DQZT状态
        // remark字段存储的是DQZTNAME（当前状态名称）
        // 如果是"D"表示注销状态，需要删除黑名单
        if ("D".equals(groupedVehicle.getRemark())) {
            log.info("车辆[{}]状态为注销(D)，执行删除黑名单操作", plateNumber);
            boolean deleteSuccess = akeVipService.deleteBlacklistByPlateNumber(plateNumber);
            if (deleteSuccess) {
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                log.info("车辆[{}]删除黑名单成功", plateNumber);
            } else {
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(), 
                        "BLACKLIST_DELETE", "注销状态删除黑名单失败");
                log.warn("车辆[{}]删除黑名单失败", plateNumber);
            }
            // 注销状态处理完删除后结束
            return deleteSuccess;
        }
        
        // 步骤2：查询ake系统现有黑名单信息
        AkeVipService.BlacklistInfo existingBlacklist = akeVipService.getBlacklistByPlateNumber(plateNumber);
        
        if (existingBlacklist != null) {
            log.info("车辆[{}]在ake系统中有黑名单记录", plateNumber);
        } else {
            log.info("车辆[{}]在ake系统中无黑名单记录", plateNumber);
        }
        
        // 步骤3和4：根据是否有现有黑名单决定处理逻辑
        if (existingBlacklist == null) {
            // 情况A：无现有黑名单，直接添加
            return addNewBlacklist(groupedVehicle, result);
        } else {
            // 情况B：有现有黑名单，需要比较权限和时间
            return updateExistingBlacklist(groupedVehicle, existingBlacklist, result);
        }
    }
    
    /**
     * 添加新黑名单（无现有黑名单的情况）
     * 
     * 合并同车牌的多个CQDMNAME为一个vip_name
     * 调用ADD_BLACK_LIST_CAR接口
     * 只调用一次接口
     * 
     * Requirements: 6.3
     */
    private boolean addNewBlacklist(GroupedVehicleInfo groupedVehicle, VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]无现有黑名单，添加新黑名单", plateNumber);

        try {
            // 构建添加请求（使用分组后的车辆信息）
            AddBlacklistCarRequest request = buildAddBlacklistRequestFromGrouped(groupedVehicle);

            if (request == null) {
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "BLACKLIST_ADD", "无法生成有效的黑名单类型");
                log.warn("车辆[{}]无法生成有效的黑名单类型", plateNumber);
                return false;
            }

            // 调用添加接口
            boolean success = akeVipService.addBlacklistCar(request);

            if (success) {
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                log.info("车辆[{}]添加黑名单成功，类型: {}", plateNumber, request.getVipTypeName());
                return true;
            } else {
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                        "BLACKLIST_ADD", "添加黑名单失败");
                log.warn("车辆[{}]添加黑名单失败", plateNumber);
                return false;
            }
        } catch (Exception e) {
            log.error("车辆[{}]添加黑名单异常: {}", plateNumber, e.getMessage(), e);
            result.setBlacklistFailed(result.getBlacklistFailed() + 1);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(),
                    "BLACKLIST_ADD", e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新现有黑名单（有现有黑名单的情况）
     * 
     * 比较Oracle权限集合和ake权限集合
     * - 权限不同：先删除再添加
     * - 权限相同：检查永久标志
     *   - 永久（blacklist_forever_flag="1"）：跳过
     *   - 临时（blacklist_forever_flag="0"）：比较时间，决定是否更新
     * 
     * Requirements: 6.2, 6.3
     */
    private boolean updateExistingBlacklist(GroupedVehicleInfo groupedVehicle, 
                                           AkeVipService.BlacklistInfo existingBlacklist,
                                           VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        log.info("车辆[{}]有现有黑名单，检查是否需要更新", plateNumber);
        
        // 提取Oracle权限集合（从orgNames转换为ake简化名称）
        Set<String> oraclePermissions = VipPermissionUtil.extractPermissionsFromOracleGateNames(
                groupedVehicle.getOrgNames());
        log.info("车辆[{}]Oracle权限: {}", plateNumber, oraclePermissions);
        
        // 提取ake权限集合（从vip_name中解析）
        Set<String> akePermissions = VipPermissionUtil.extractPermissionsFromBlacklistType(
                existingBlacklist.getVipName());
        log.info("车辆[{}]ake权限: {}", plateNumber, akePermissions);
        
        // 比较权限
        boolean permissionsEqual = VipPermissionUtil.arePermissionsEqual(oraclePermissions, akePermissions);
        
        if (!permissionsEqual) {
            // 权限不同：先删除再添加
            log.info("车辆[{}]权限变化，执行删除+添加流程", plateNumber);
            
            try {
                // 先删除
                boolean deleteSuccess = akeVipService.deleteBlacklistCar(
                        existingBlacklist.getBlacklistSeq(), null, null);
                
                if (deleteSuccess) {
                    log.info("车辆[{}]删除黑名单成功", plateNumber);
                } else {
                    log.warn("车辆[{}]删除黑名单失败，继续尝试添加", plateNumber);
                }
                
                // 再添加新黑名单（使用新权限）
                return addNewBlacklist(groupedVehicle, result);
                
            } catch (Exception e) {
                log.error("车辆[{}]权限变化处理异常: {}", plateNumber, e.getMessage(), e);
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(), 
                        "BLACKLIST_UPDATE", e.getMessage());
                return false;
            }
        } else {
            // 权限相同：检查永久标志和时间
            log.info("车辆[{}]权限相同，检查时间是否需要更新", plateNumber);
            return checkAndUpdateBlacklistTime(groupedVehicle, existingBlacklist, result);
        }
    }
    
    /**
     * 检查并更新黑名单时间（权限相同时）
     * 
     * 检查blacklist_forever_flag：
     * - "1"（永久）：跳过
     * - "0"（临时）：比较timeperiod_list和Oracle时间
     *   - 时间不同：先删除再添加
     *   - 时间相同：跳过
     * 
     * Requirements: 6.3
     */
    private boolean checkAndUpdateBlacklistTime(GroupedVehicleInfo groupedVehicle,
                                               AkeVipService.BlacklistInfo existingBlacklist,
                                               VehicleSyncResult result) {
        String plateNumber = groupedVehicle.getPlateNumber();
        
        try {
            // 检查永久标志
            String foreverFlag = existingBlacklist.getBlacklistForeverFlag();
            
            if ("1".equals(foreverFlag)) {
                // 永久黑名单，跳过
                log.info("车辆[{}]为永久黑名单，无需更新时间", plateNumber);
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                return true;
            }
            
            // 临时黑名单，比较时间
            log.info("车辆[{}]为临时黑名单，比较时间", plateNumber);
            
            // 获取Oracle的时间
            LocalDateTime oracleStartTime = groupedVehicle.getValidStartTime();
            LocalDateTime oracleEndTime = groupedVehicle.getValidEndTime();
            
            if (oracleStartTime == null || oracleEndTime == null) {
                log.warn("车辆[{}]Oracle时间为空，跳过时间更新", plateNumber);
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                return true;
            }
            
            // 获取ake系统的时间（格式：开始时间~结束时间）
            String timeperiodList = existingBlacklist.getTimeperiodList();
            
            if (timeperiodList == null || timeperiodList.trim().isEmpty()) {
                log.warn("车辆[{}]ake时间为空，跳过时间更新", plateNumber);
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                return true;
            }
            
            // 解析ake时间
            String[] timeParts = timeperiodList.split("~");
            if (timeParts.length != 2) {
                log.warn("车辆[{}]ake时间格式错误: {}", plateNumber, timeperiodList);
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                return true;
            }
            
            String akeStartTime = timeParts[0].trim();
            String akeEndTime = timeParts[1].trim();
            
            // 格式化Oracle时间为字符串（用于比较）
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String oracleStartStr = oracleStartTime.format(formatter);
            String oracleEndStr = oracleEndTime.format(formatter);
            
            log.info("车辆[{}]时间比较 - Oracle: {} ~ {}, ake: {} ~ {}", 
                    plateNumber, oracleStartStr, oracleEndStr, akeStartTime, akeEndTime);
            
            // 比较时间（允许秒级误差）
            boolean timeEqual = isTimeEqual(oracleStartStr, akeStartTime) && 
                               isTimeEqual(oracleEndStr, akeEndTime);
            
            if (timeEqual) {
                // 时间相同，跳过
                log.info("车辆[{}]时间相同，无需更新", plateNumber);
                result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                return true;
            } else {
                // 时间不同，先删除再添加
                log.info("车辆[{}]时间不同，执行删除+添加流程", plateNumber);
                
                // 先删除
                boolean deleteSuccess = akeVipService.deleteBlacklistCar(
                        existingBlacklist.getBlacklistSeq(), null, null);
                
                if (deleteSuccess) {
                    log.info("车辆[{}]删除黑名单成功", plateNumber);
                } else {
                    log.warn("车辆[{}]删除黑名单失败，继续尝试添加", plateNumber);
                }
                
                // 再添加新黑名单（使用新时间）
                return addNewBlacklist(groupedVehicle, result);
            }
        } catch (Exception e) {
            log.error("车辆[{}]时间更新异常: {}", plateNumber, e.getMessage(), e);
            result.setBlacklistFailed(result.getBlacklistFailed() + 1);
            result.addFailedRecord(plateNumber, groupedVehicle.getOwnerName(), 
                    "BLACKLIST_TIME_UPDATE", e.getMessage());
            return false;
        }
    }
    
    /**
     * 根据分组后的车辆信息构建添加黑名单请求
     *
     * 关键点：
     * - 合并同车牌的多个CQDMNAME为一个vip_name
     * - 使用智能匹配，只保留威尔系统中实际存在的门（化工西、化肥西、复合肥南）
     * - 黑名单类型名称格式：厂区组合 + "VIP"
     * - 根据有效期判断是临时还是永久黑名单
     *
     * @param groupedVehicle 分组后的车辆信息
     * @return 添加黑名单请求
     */
    private AddBlacklistCarRequest buildAddBlacklistRequestFromGrouped(GroupedVehicleInfo groupedVehicle) {
        AddBlacklistCarRequest request = new AddBlacklistCarRequest();

        // 合并权限为黑名单类型名称
        Set<String> permissions = VipPermissionUtil.extractPermissionsFromOracleGateNames(
                groupedVehicle.getOrgNames());

        // 使用智能匹配，只保留威尔系统中实际存在的门
        // 如果Oracle权限包含不存在的门（如：化三南、化工东、炼油南等），会被自动过滤
        // 生成的黑名单类型名称会匹配已知的AKE VIP类型
        String vipName = VipTypeMatcherUtil.findBestMatchVipTypeFromPermissions(permissions);

        if (vipName == null || vipName.trim().isEmpty()) {
            log.warn("车辆[{}]无法生成有效的黑名单类型，Oracle权限: {}",
                    groupedVehicle.getPlateNumber(), permissions);
            return null;
        }

        log.info("车辆[{}]Oracle权限: {} -> 智能匹配后黑名单类型: {}",
                groupedVehicle.getPlateNumber(), permissions, vipName);

        request.setVipTypeCode(""); // 由系统自动分配
        request.setVipTypeName(vipName);
        request.setCarCode(groupedVehicle.getPlateNumber());
        request.setCarOwner(groupedVehicle.getOwnerName());
        request.setReason(groupedVehicle.getOwnerPhone() != null ? groupedVehicle.getOwnerPhone() : "请停车检查");

        // 根据有效期判断是临时还是永久黑名单
        if (groupedVehicle.getValidEndTime() != null) {
            // 有结束时间，设置为临时黑名单
            request.setIsPermament(0);

            AddBlacklistCarRequest.TimePeriod timePeriod = new AddBlacklistCarRequest.TimePeriod();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            if (groupedVehicle.getValidStartTime() != null) {
                timePeriod.setStartTime(groupedVehicle.getValidStartTime().format(formatter));
            } else {
                timePeriod.setStartTime(LocalDateTime.now().format(formatter));
            }
            timePeriod.setEndTime(groupedVehicle.getValidEndTime().format(formatter));

            request.setTimePeriod(timePeriod);
        } else {
            // 无结束时间，设置为永久黑名单
            request.setIsPermament(1);
        }

        request.setRemark1(groupedVehicle.getRemark());
        request.setRemark2("Oracle数据同步");
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());

        return request;
    }
    
    /**
     * 根据分组后的车辆信息构建开通VIP票请求
     *
     * 关键点：
     * - 合并同车牌的多个CQDMNAME为一个vip_type_name
     * - 使用智能匹配，只保留AKE系统中实际存在的门（化工西、化肥西、复合肥南）
     * - VIP类型名称格式：请停车检查（厂区组合）
     * - 所有操作都是0元
     *
     * @param groupedVehicle 分组后的车辆信息
     * @return 开通VIP票请求
     */
    private OpenVipTicketRequest buildOpenVipTicketRequestFromGrouped(GroupedVehicleInfo groupedVehicle) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();

        // 合并权限为VIP类型名称
        Set<String> permissions = VipPermissionUtil.extractPermissionsFromOracleGateNames(
                groupedVehicle.getOrgNames());

        // 使用智能匹配，只保留AKE系统中实际存在的门
        // 如果Oracle权限包含不存在的门（如：化三南、化工东、炼油南等），会被自动过滤
        // 生成的VIP类型名称会匹配已知的AKE VIP类型
        String vipTypeName = VipTypeMatcherUtil.findBestMatchBlacklistType(permissions);

        if (vipTypeName == null || vipTypeName.trim().isEmpty()) {
            log.warn("车辆[{}]无法生成有效的VIP类型，Oracle权限: {}",
                    groupedVehicle.getPlateNumber(), permissions);
            return null;
        }

        log.info("车辆[{}]Oracle权限: {} -> 智能匹配后VIP类型: {}",
                groupedVehicle.getPlateNumber(), permissions, vipTypeName);

        request.setVipTypeName(vipTypeName);
        request.setTicketNo(generateTicketNo(groupedVehicle.getPlateNumber()));
        // 车主姓名：如果查询不到则使用车牌号码
        String carOwner = groupedVehicle.getOwnerName();
        if (carOwner == null || carOwner.trim().isEmpty()) {
            carOwner = groupedVehicle.getPlateNumber();
        }
        request.setCarOwner(carOwner);
        // 车主电话：如果查询不到则使用时间戳生成唯一号码（格式：13+时间戳后9位）
        String telphone = groupedVehicle.getOwnerPhone();
        if (telphone == null || telphone.trim().isEmpty()) {
            telphone = "13" + String.valueOf(System.currentTimeMillis()).substring(4);
        }
        request.setTelphone(telphone);
        request.setCompany(groupedVehicle.getCompany());
        request.setDepartment(groupedVehicle.getCompany());
        request.setSex("0");
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());
        request.setOriginalPrice("0");
        request.setDiscountPrice("0");
        request.setOpenValue("1");
        request.setOpenCarCount("1");

        // 车辆列表
        List<OpenVipTicketRequest.CarInfo> carList = new ArrayList<>();
        OpenVipTicketRequest.CarInfo carInfo = new OpenVipTicketRequest.CarInfo();
        carInfo.setCarNo(groupedVehicle.getPlateNumber());
        carList.add(carInfo);
        request.setCarList(carList);

        // 时间段列表（有效期）
        List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
        OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (groupedVehicle.getValidStartTime() != null) {
            timePeriod.setStartTime(groupedVehicle.getValidStartTime().format(formatter));
        } else {
            timePeriod.setStartTime(LocalDateTime.now().format(formatter));
        }

        if (groupedVehicle.getValidEndTime() != null) {
            timePeriod.setEndTime(groupedVehicle.getValidEndTime().format(formatter));
        } else {
            timePeriod.setEndTime(LocalDateTime.now().plusYears(1).format(formatter));
        }

        timePeriodList.add(timePeriod);
        request.setTimePeriodList(timePeriodList);

        return request;
    }
    
    /**
     * 生成票号
     * 格式：车牌号_时间戳
     */
    private String generateTicketNo(String plateNumber) {
        return plateNumber + "_" + System.currentTimeMillis();
    }
    
    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 处理单个车辆数据
     * 
     * Requirements: 5.1, 5.2, 5.3, 5.4, 6.1, 6.2
     */
    private boolean processVehicle(OracleVehicleInfo vehicle, VehicleSyncResult result) {
        String plateNumber = vehicle.getPlateNumber();
        log.info("处理车辆: {}, 车主: {}, VIP类型: {}, 需要检查: {}", 
                plateNumber, vehicle.getOwnerName(), vehicle.getVipTypeName(), vehicle.isNeedCheck());
        boolean success = true;
        // 1. 处理VIP月票
        if (StringUtils.hasText(vehicle.getVipTypeName())) {
            try {
                // 检查是否已有VIP票，如有则先退票
                if (akeVipService.hasActiveVipTicket(plateNumber)) {
                    log.info("车辆[{}]已有VIP票，先进行退票", plateNumber);
                    boolean refundSuccess = akeVipService.refundVipTicketByPlateNumber(plateNumber);
                    if (refundSuccess) {
                        result.setVipRefundSuccess(result.getVipRefundSuccess() + 1);
                    } else {
                        result.setVipRefundFailed(result.getVipRefundFailed() + 1);
                        log.warn("车辆[{}]退票失败，继续尝试开通新VIP", plateNumber);
                    }
                }
                // 开通VIP月票
                boolean openSuccess = akeVipService.openVipTicketFromOracle(vehicle);
                if (openSuccess) {
                    result.setVipOpenSuccess(result.getVipOpenSuccess() + 1);
                    log.info("车辆[{}]开通VIP月票成功", plateNumber);
                } else {
                    result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                    result.addFailedRecord(plateNumber, vehicle.getOwnerName(), 
                            "VIP_OPEN", "开通VIP月票失败");
                    success = false;
                }
            } catch (Exception e) {
                log.error("车辆[{}]VIP处理异常: {}", plateNumber, e.getMessage());
                result.setVipOpenFailed(result.getVipOpenFailed() + 1);
                result.addFailedRecord(plateNumber, vehicle.getOwnerName(), 
                        "VIP_OPEN", e.getMessage());
                success = false;
            }
        }

        // 2. 处理黑名单
        if (vehicle.isNeedCheck()) {
            try {
                boolean blacklistSuccess = akeVipService.addBlacklistFromOracle(vehicle);
                if (blacklistSuccess) {
                    result.setBlacklistSuccess(result.getBlacklistSuccess() + 1);
                    log.info("车辆[{}]添加黑名单成功", plateNumber);
                } else {
                    result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                    result.addFailedRecord(plateNumber, vehicle.getOwnerName(), 
                            "BLACKLIST", "添加黑名单失败");
                    // 黑名单失败不影响整体成功状态
                }
            } catch (Exception e) {
                log.error("车辆[{}]黑名单处理异常: {}", plateNumber, e.getMessage());
                result.setBlacklistFailed(result.getBlacklistFailed() + 1);
                result.addFailedRecord(plateNumber, vehicle.getOwnerName(), 
                        "BLACKLIST", e.getMessage());
            }
        }

        return success;
    }


    @Override
    public LocalDateTime getLastSyncTime() {
        try {
            Path path = Paths.get(lastSyncTimeFile);
            if (!Files.exists(path)) {
                // 第一次运行：从2026年1月6日开始同步
                LocalDateTime firstSyncTime = LocalDateTime.of(2026, 1, 6, 0, 0, 0);
                log.info("同步时间文件不存在，首次运行，从2026-01-06开始同步: {}", firstSyncTime);
                return firstSyncTime;
            }
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
            if (!StringUtils.hasText(content)) {
                // 文件为空：从2026年1月6日开始同步
                LocalDateTime firstSyncTime = LocalDateTime.of(2026, 1, 6, 0, 0, 0);
                log.info("同步时间文件为空，从2026-01-06开始同步: {}", firstSyncTime);
                return firstSyncTime;
            }
            LocalDateTime lastSyncTime = LocalDateTime.parse(content, DATE_TIME_FORMATTER);
            log.debug("读取上次同步时间: {}", lastSyncTime);
            return lastSyncTime;
        } catch (Exception e) {
            // 异常情况：从2026年1月6日开始同步
            LocalDateTime firstSyncTime = LocalDateTime.of(2026, 1, 6, 0, 0, 0);
            log.error("读取同步时间文件失败: {}，使用2026-01-06作为起始时间", e.getMessage());
            return firstSyncTime;
        }
    }

    @Override
    public void updateLastSyncTime(LocalDateTime time) {
        try {
            Path path = Paths.get(lastSyncTimeFile);
            // 确保父目录存在
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                log.info("创建同步时间文件目录: {}", parentDir);
            }
            // 写入同步时间
            String content = time.format(DATE_TIME_FORMATTER);
            Files.write(path, content.getBytes(StandardCharsets.UTF_8));
            log.info("更新同步时间: {}", content);
        } catch (Exception e) {
            log.error("更新同步时间文件失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public boolean isSyncRunning() {
        return syncRunning.get();
    }
}
