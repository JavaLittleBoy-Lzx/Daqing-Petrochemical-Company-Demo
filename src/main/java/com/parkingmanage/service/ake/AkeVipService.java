package com.parkingmanage.service.ake;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.parkingmanage.common.HttpClientUtil;
import com.parkingmanage.dto.ake.AddBlacklistCarRequest;
import com.parkingmanage.dto.ake.AddVisitorCarRequest;
import com.parkingmanage.dto.ake.OpenVipTicketRequest;
import com.parkingmanage.util.VipTypeMatcherUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AKE车辆服务
 * 负责调用AKE停车系统接口
 */
@Slf4j
@Service
public class AkeVipService {

    @Value("${ake.api.base-url}")
    private String baseUrl;

    @Value("${ake.api.app-key}")
    private String appKey;

    @Value("${ake.default-operator:系统同步}")
    private String defaultOperator;

    private static final String API_PATH = "/cxfService/external/extReq";

    /**
     * 查询VIP车辆信息 (GET_VIP_TICKET - 4.10)
     * 
     * @param plateNumber 车牌号
     * @param carOwner 车主姓名
     * @param vipTypeName VIP类型名称
     * @return VIP票信息列表
     */
    public List<VipTicketInfo> getVipTicket(String plateNumber, String carOwner, String vipTypeName) {
        log.info("查询VIP票信息，车牌: {}, 车主: {}, VIP类型: {}", plateNumber, carOwner, vipTypeName);

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("vip_type_name", vipTypeName != null ? vipTypeName : "");
            bizContent.put("car_owner", carOwner != null ? carOwner : "");
            bizContent.put("car_no", plateNumber != null ? plateNumber : "");
            bizContent.put("page_num", "1");
            bizContent.put("page_size", "100");

            String response = callAkeApi("GET_VIP_TICKET", bizContent);
            return parseVipTicketResponse(response);
        } catch (Exception e) {
            log.error("查询VIP票信息失败，车牌: {}", plateNumber, e);
            return new ArrayList<>();
        }
    }

    /**
     * VIP票退费 (REFUND_VIP_TICKET - 4.12)
     * 
     * @param vipTicketSeq VIP票序列号
     * @param operator 操作人
     * @param operateTime 操作时间
     * @param refundPrice 退费金额
     * @return 是否成功
     */
    public boolean refundVipTicket(String vipTicketSeq, String operator, String operateTime, String refundPrice) {
        log.info("VIP票退费，票序列号: {}, 操作人: {}", vipTicketSeq, operator);

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("vip_ticket_seq", vipTicketSeq);
            bizContent.put("operator", operator != null ? operator : defaultOperator);
            bizContent.put("operate_time", operateTime != null ? operateTime : getCurrentTime());
            bizContent.put("refund_price", refundPrice != null ? refundPrice : "0");

            String response = callAkeApi("REFUND_VIP_TICKET", bizContent);
            return parseSuccessResponse(response, "VIP票退费");
        } catch (Exception e) {
            log.error("VIP票退费失败，票序列号: {}", vipTicketSeq, e);
            return false;
        }
    }

    /**
     * VIP票续费 (RENEW_VIP_TICKET)
     * 
     * @param vipTicketSeq VIP票序列号
     * @param startTime 新的开始时间
     * @param endTime 新的结束时间
     * @param operator 操作人
     * @param operateTime 操作时间
     * @return 是否成功
     */
    public boolean renewVipTicket(String vipTicketSeq, String startTime, String endTime, String operator, String operateTime) {
        log.info("VIP票续费，票序列号: {}, 新有效期: {} ~ {}", vipTicketSeq, startTime, endTime);

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("vip_ticket_seq", vipTicketSeq);
            bizContent.put("start_time", startTime);
            bizContent.put("end_time", endTime);
            bizContent.put("operator", operator != null ? operator : defaultOperator);
            bizContent.put("operate_time", operateTime != null ? operateTime : getCurrentTime());
            bizContent.put("renew_price", "0"); // 续费金额固定为0元

            String response = callAkeApi("RENEW_VIP_TICKET", bizContent);
            return parseSuccessResponse(response, "VIP票续费");
        } catch (Exception e) {
            log.error("VIP票续费失败，票序列号: {}", vipTicketSeq, e);
            return false;
        }
    }

    /**
     * 根据车牌号退票
     * 先查询车辆的VIP票，然后逐一退票
     * 
     * @param plateNumber 车牌号
     * @return 退票结果：true-成功或无需退票，false-退票失败
     */
    public boolean refundVipTicketByPlateNumber(String plateNumber) {
        log.info("根据车牌号退票，车牌: {}", plateNumber);

        try {
            // 1. 查询该车牌的所有VIP票
            List<VipTicketInfo> tickets = getVipTicket(plateNumber, null, null);
            
            if (tickets == null || tickets.isEmpty()) {
                log.info("车牌 {} 无VIP票，无需退票", plateNumber);
                return true;
            }

            // 2. 筛选生效中的票进行退票
            boolean allSuccess = true;
            int refundCount = 0;
            for (VipTicketInfo ticket : tickets) {
                // 只退"生效中"状态的票
                if ("生效中".equals(ticket.getTicketStatus()) || "1".equals(ticket.getTicketStatus())) {
                    log.info("退票: 车牌={}, 票序列号={}, VIP类型={}", 
                            plateNumber, ticket.getVipTicketSeq(), ticket.getVipTypeName());
                    
                    boolean success = refundVipTicket(ticket.getVipTicketSeq(), null, null, "0");
                    if (!success) {
                        log.warn("退票失败: 车牌={}, 票序列号={}", plateNumber, ticket.getVipTicketSeq());
                        allSuccess = false;
                    } else {
                        refundCount++;
                    }
                }
            }

            log.info("车牌 {} 退票完成，共退 {} 张票，结果: {}", plateNumber, refundCount, allSuccess ? "成功" : "部分失败");
            return allSuccess;
        } catch (Exception e) {
            log.error("根据车牌号退票失败，车牌: {}", plateNumber, e);
            return false;
        }
    }

    /**
     * 检查车辆是否有生效中的VIP票
     * 
     * @param plateNumber 车牌号
     * @return true-有生效中的VIP票，false-无
     */
    public boolean hasActiveVipTicket(String plateNumber) {
        try {
            List<VipTicketInfo> tickets = getVipTicket(plateNumber, null, null);
            if (tickets == null || tickets.isEmpty()) {
                return false;
            }
            
            return tickets.stream()
                    .anyMatch(t -> "生效中".equals(t.getTicketStatus()) || "1".equals(t.getTicketStatus()));
        } catch (Exception e) {
            log.error("检查VIP票状态失败，车牌: {}", plateNumber, e);
            return false;
        }
    }

    /**
     * 开通VIP月票 (OPEN_VIP_TICKET - 4.2)
     * 
     * @param request 开通请求
     * @return 是否成功
     */
    public boolean openVipTicket(OpenVipTicketRequest request) {
        log.info("开通VIP月票，车牌: {}, 车主: {}, VIP类型: {}", 
                request.getCarList() != null && !request.getCarList().isEmpty() ? 
                    request.getCarList().get(0).getCarNo() : "无",
                request.getCarOwner(), 
                request.getVipTypeName());

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("vip_type_name", request.getVipTypeName());
            bizContent.put("ticket_no", request.getTicketNo() != null ? request.getTicketNo() : "");
            bizContent.put("car_owner", request.getCarOwner());
            bizContent.put("telphone", request.getTelphone() != null ? request.getTelphone() : "");
            bizContent.put("company", request.getCompany() != null ? request.getCompany() : "");
            bizContent.put("department", request.getDepartment() != null ? request.getDepartment() : "");
            bizContent.put("sex", request.getSex() != null ? request.getSex() : "0");
            bizContent.put("operator", request.getOperator() != null ? request.getOperator() : defaultOperator);
            bizContent.put("operate_time", request.getOperateTime() != null ? request.getOperateTime() : getCurrentTime());
            bizContent.put("original_price", request.getOriginalPrice() != null ? request.getOriginalPrice() : "0");
            bizContent.put("discount_price", request.getDiscountPrice() != null ? request.getDiscountPrice() : "0");
            bizContent.put("open_value", request.getOpenValue() != null ? request.getOpenValue() : "1");
            bizContent.put("open_car_count", request.getOpenCarCount() != null ? request.getOpenCarCount() : "1");

            // 车辆列表
            List<Map<String, String>> carList = new ArrayList<>();
            if (request.getCarList() != null) {
                for (OpenVipTicketRequest.CarInfo car : request.getCarList()) {
                    Map<String, String> carMap = new HashMap<>();
                    carMap.put("car_no", car.getCarNo());
                    carList.add(carMap);
                }
            }
            bizContent.put("car_list", carList);

            // 时间段列表
            List<Map<String, String>> timePeriodList = new ArrayList<>();
            if (request.getTimePeriodList() != null) {
                for (OpenVipTicketRequest.TimePeriod period : request.getTimePeriodList()) {
                    Map<String, String> periodMap = new HashMap<>();
                    periodMap.put("start_time", period.getStartTime());
                    periodMap.put("end_time", period.getEndTime());
                    timePeriodList.add(periodMap);
                }
            }
            bizContent.put("time_period_list", timePeriodList);

            String response = callAkeApi("OPEN_VIP_TICKET", bizContent);
            boolean success = parseSuccessResponse(response, "开通VIP月票");
            
            // 如果失败且错误信息包含"卡类型不存在"，尝试智能匹配
            if (!success && response != null && response.contains("卡类型不存在")) {
                log.warn("VIP类型[{}]不存在，尝试智能匹配", request.getVipTypeName());
                String matchedVipType = VipTypeMatcherUtil.findBestMatchVipType(request.getVipTypeName());
                
                if (matchedVipType != null && !matchedVipType.equals(request.getVipTypeName())) {
                    log.info("找到匹配的VIP类型: {}，重新尝试开通", matchedVipType);
                    
                    // 使用匹配的VIP类型重新尝试
                    request.setVipTypeName(matchedVipType);
                    bizContent.put("vip_type_name", matchedVipType);
                    
                    String retryResponse = callAkeApi("OPEN_VIP_TICKET", bizContent);
                    success = parseSuccessResponse(retryResponse, "开通VIP月票（智能匹配）");
                    
                    if (success) {
                        log.info("使用智能匹配的VIP类型[{}]开通成功", matchedVipType);
                    }
                }
            }
            
            return success;
        } catch (Exception e) {
            log.error("开通VIP月票失败", e);
            return false;
        }
    }

    /**
     * 根据Oracle车辆信息开通VIP月票
     * 会自动处理退票逻辑：如果已有VIP票则先退票再开通
     * 
     * @param vehicleInfo Oracle车辆信息
     * @return 是否成功
     */
    public boolean openVipTicketFromOracle(com.parkingmanage.entity.OracleVehicleInfo vehicleInfo) {
        String plateNumber = vehicleInfo.getPlateNumber();
        log.info("从Oracle数据开通VIP月票，车牌: {}, 车主: {}, VIP类型: {}", 
                plateNumber, vehicleInfo.getOwnerName(), vehicleInfo.getVipTypeName());

        try {
            // 1. 检查是否已有VIP票，如有则先退票
            if (hasActiveVipTicket(plateNumber)) {
                log.info("车牌 {} 已有VIP票，先进行退票", plateNumber);
                boolean refundSuccess = refundVipTicketByPlateNumber(plateNumber);
                if (!refundSuccess) {
                    log.warn("车牌 {} 退票失败，继续尝试开通新VIP", plateNumber);
                }
            }

            // 2. 构建开通请求
            OpenVipTicketRequest request = buildOpenVipTicketRequest(vehicleInfo);

            // 3. 开通VIP月票
            boolean success = openVipTicket(request);
            
            if (success) {
                log.info("车牌 {} 开通VIP月票成功，有效期: {} ~ {}", 
                        plateNumber, vehicleInfo.getValidStartTime(), vehicleInfo.getValidEndTime());
            } else {
                log.warn("车牌 {} 开通VIP月票失败", plateNumber);
            }
            
            return success;
        } catch (Exception e) {
            log.error("从Oracle数据开通VIP月票失败，车牌: {}", plateNumber, e);
            return false;
        }
    }

    /**
     * 根据Oracle车辆信息构建开通VIP票请求
     * 
     * @param vehicleInfo Oracle车辆信息
     * @return 开通VIP票请求
     */
    private OpenVipTicketRequest buildOpenVipTicketRequest(com.parkingmanage.entity.OracleVehicleInfo vehicleInfo) {
        OpenVipTicketRequest request = new OpenVipTicketRequest();
        
        // 基本信息 - VIP类型固定为"化工西门VIP"
        request.setVipTypeName("化工西门VIP");
        request.setTicketNo(generateTicketNo(vehicleInfo.getPlateNumber()));
        // 车主姓名：如果查询不到则使用车牌号码
        String carOwner = vehicleInfo.getOwnerName();
        if (carOwner == null || carOwner.trim().isEmpty()) {
            carOwner = vehicleInfo.getPlateNumber();
        }
        request.setCarOwner(carOwner);
        // 车主电话：如果查询不到则使用时间戳生成唯一号码（格式：13+时间戳后9位）
        String telphone = vehicleInfo.getOwnerPhone();
        if (telphone == null || telphone.trim().isEmpty()) {
            telphone = "13" + String.valueOf(System.currentTimeMillis()).substring(4);
        }
        request.setTelphone(telphone);
        request.setCompany(vehicleInfo.getCompany());
        request.setDepartment(vehicleInfo.getCompany()); // 部门使用单位名称
        request.setSex("0"); // 默认男
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());
        request.setOriginalPrice("0");
        request.setDiscountPrice("0");
        request.setOpenValue("1");
        request.setOpenCarCount("1");

        // 车辆列表
        List<OpenVipTicketRequest.CarInfo> carList = new ArrayList<>();
        OpenVipTicketRequest.CarInfo carInfo = new OpenVipTicketRequest.CarInfo();
        carInfo.setCarNo(vehicleInfo.getPlateNumber());
        carList.add(carInfo);
        request.setCarList(carList);

        // 时间段列表（有效期）
        List<OpenVipTicketRequest.TimePeriod> timePeriodList = new ArrayList<>();
        OpenVipTicketRequest.TimePeriod timePeriod = new OpenVipTicketRequest.TimePeriod();
        
        // 格式化有效期时间
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        if (vehicleInfo.getValidStartTime() != null) {
            timePeriod.setStartTime(vehicleInfo.getValidStartTime().format(formatter));
        } else {
            timePeriod.setStartTime(LocalDateTime.now().format(formatter));
        }
        
        if (vehicleInfo.getValidEndTime() != null) {
            timePeriod.setEndTime(vehicleInfo.getValidEndTime().format(formatter));
        } else {
            // 默认一年有效期
            timePeriod.setEndTime(LocalDateTime.now().plusYears(1).format(formatter));
        }
        
        timePeriodList.add(timePeriod);
        request.setTimePeriodList(timePeriodList);

        return request;
    }

    /**
     * 生成票号
     * 格式：车牌号_时间戳
     * 
     * @param plateNumber 车牌号
     * @return 票号
     */
    private String generateTicketNo(String plateNumber) {
        return plateNumber + "_" + System.currentTimeMillis();
    }

    /**
     * 查询黑名单列表 (GET_BLACK_LIST)
     * 使用分页查询获取所有黑名单
     *
     * @return 黑名单信息列表
     */
    public List<BlacklistInfo> getBlacklistCars() {
        log.info("查询黑名单列表");
        // 使用分页查询方法
        return getAllBlacklistsByPage();
    }

    /**
     * 分页查询所有黑名单 (GET_BLACK_LIST)
     * 通过多次分页查询获取所有黑名单数据
     *
     * @return 黑名单信息列表
     */
    public List<BlacklistInfo> getAllBlacklistsByPage() {
        log.info("开始分页查询所有黑名单");

        List<BlacklistInfo> allBlacklists = new ArrayList<>();
        int pageSize = 100; // 每页100条
        int pageNumber = 1;
        boolean hasMore = true;

        try {
            while (hasMore) {
                Map<String, Object> bizContent = new HashMap<>();
                bizContent.put("page_size", pageSize);
                bizContent.put("page_number", pageNumber);

                String response = callAkeApi("GET_BLACK_LIST", bizContent);
                List<BlacklistInfo> pageData = parseBlacklistResponse(response);

                if (pageData == null || pageData.isEmpty()) {
                    hasMore = false;
                } else {
                    allBlacklists.addAll(pageData);
                    // 如果返回的数据少于pageSize，说明已经是最后一页
                    if (pageData.size() < pageSize) {
                        hasMore = false;
                    } else {
                        pageNumber++;
                    }
                }
            }

            log.info("分页查询黑名单完成，共获取 {} 条记录", allBlacklists.size());
            return allBlacklists;

        } catch (Exception e) {
            log.error("分页查询黑名单失败", e);
            return allBlacklists;
        }
    }

    /**
     * 根据车牌号查询黑名单
     * 直接使用车牌号参数查询 GET_BLACK_LIST
     *
     * @param plateNumber 车牌号
     * @return 黑名单信息，如果不存在返回null
     */
    public BlacklistInfo getBlacklistByPlateNumber(String plateNumber) {
        log.info("根据车牌号查询黑名单，车牌: {}", plateNumber);

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("car_license_number", plateNumber);
            bizContent.put("page_size", 10);
            bizContent.put("page_number", 1);

            String response = callAkeApi("GET_BLACK_LIST", bizContent);
            List<BlacklistInfo> blacklists = parseBlacklistResponse(response);

            if (blacklists == null || blacklists.isEmpty()) {
                log.info("车牌 {} 不在黑名单中", plateNumber);
                return null;
            }

            BlacklistInfo blacklist = blacklists.get(0);
            log.info("找到车牌 {} 的黑名单记录", plateNumber);
            return blacklist;
        } catch (Exception e) {
            log.error("根据车牌号查询黑名单失败，车牌: {}", plateNumber, e);
            return null;
        }
    }

    /**
     * 添加黑名单 (ADD_BLACK_LIST_CAR - 4.17)
     * 
     * @param request 添加请求
     * @return 是否成功
     */
    public boolean addBlacklistCar(AddBlacklistCarRequest request) {
        log.info("添加黑名单，车牌: {}, 原因: {}", request.getCarCode(), request.getReason());

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("vip_type_code", request.getVipTypeCode() != null ? request.getVipTypeCode() : "");
            bizContent.put("vip_type_name", request.getVipTypeName() != null ? request.getVipTypeName() : "黑名单");
            bizContent.put("car_code", request.getCarCode());
            bizContent.put("car_owner", request.getCarOwner() != null ? request.getCarOwner() : "");
            bizContent.put("reason", request.getReason() != null ? request.getReason() : "请停车检查");
            bizContent.put("is_permament", request.getIsPermament() != null ? request.getIsPermament() : 0);

            // 时间段
            if (request.getTimePeriod() != null) {
                Map<String, String> timePeriod = new HashMap<>();
                timePeriod.put("start_time", request.getTimePeriod().getStartTime());
                timePeriod.put("end_time", request.getTimePeriod().getEndTime());
                bizContent.put("time_period", timePeriod);
            }

            bizContent.put("remark1", request.getRemark1() != null ? request.getRemark1() : "");
            bizContent.put("remark2", request.getRemark2() != null ? request.getRemark2() : "");
            bizContent.put("operator", request.getOperator() != null ? request.getOperator() : defaultOperator);
            bizContent.put("operate_time", request.getOperateTime() != null ? request.getOperateTime() : getCurrentTime());

            String response = callAkeApi("ADD_BLACK_LIST_CAR", bizContent);
            return parseSuccessResponse(response, "添加黑名单");
        } catch (Exception e) {
            log.error("添加黑名单失败，车牌: {}", request.getCarCode(), e);
            return false;
        }
    }

    /**
     * 添加访客车辆 (ADD_VISITOR_CAR)
     *
     * @param request 添加访客请求
     * @return 是否成功
     */
    public boolean addVisitorCar(AddVisitorCarRequest request) {
        log.info("添加访客车辆，车牌: {}, 访客类型: {}", request.getCarCode(), request.getVisitName());

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("car_code", request.getCarCode());
            bizContent.put("owner", request.getOwner() != null ? request.getOwner() : "");
            bizContent.put("visit_name", request.getVisitName());
            bizContent.put("phonenum", request.getPhonenum() != null ? request.getPhonenum() : "");
            bizContent.put("reason", request.getReason() != null ? request.getReason() : "来访");
            bizContent.put("operator", request.getOperator() != null ? request.getOperator() : defaultOperator);
            bizContent.put("operate_time", request.getOperateTime() != null ? request.getOperateTime() : getCurrentTime());

            // 访问时间
            if (request.getVisitTime() != null) {
                Map<String, String> visitTime = new HashMap<>();
                visitTime.put("start_time", request.getVisitTime().getStartTime());
                visitTime.put("end_time", request.getVisitTime().getEndTime());
                bizContent.put("visit_time", visitTime);
            }

            String response = callAkeApi("ADD_VISITOR_CAR", bizContent);
            return parseSuccessResponse(response, "添加访客车辆");
        } catch (Exception e) {
            log.error("添加访客车辆失败，车牌: {}", request.getCarCode(), e);
            return false;
        }
    }

    /**
     * 删除黑名单 (REMOVE_BLACK_LIST_CAR)
     *
     * @param carCode 车牌号
     * @param operator 操作人
     * @param operateTime 操作时间
     * @return 是否成功
     */
    public boolean deleteBlacklistCar(String carCode, String operator, String operateTime) {
        log.info("删除黑名单，车牌: {}", carCode);

        try {
            Map<String, Object> bizContent = new HashMap<>();
            bizContent.put("car_code", carCode);
            bizContent.put("operator", operator != null ? operator : defaultOperator);
            bizContent.put("operate_time", operateTime != null ? operateTime : getCurrentTime());

            String response = callAkeApi("REMOVE_BLACK_LIST_CAR", bizContent);
            return parseSuccessResponse(response, "删除黑名单");
        } catch (Exception e) {
            log.error("删除黑名单失败，车牌: {}", carCode, e);
            return false;
        }
    }

    /**
     * 根据车牌号删除黑名单
     * 直接调用删除接口，无需先查询
     *
     * @param plateNumber 车牌号
     * @return 是否成功
     */
    public boolean deleteBlacklistByPlateNumber(String plateNumber) {
        log.info("根据车牌号删除黑名单，车牌: {}", plateNumber);

        try {
            // 直接使用车牌号删除，无需先查询
            boolean success = deleteBlacklistCar(plateNumber, null, null);

            if (success) {
                log.info("车牌 {} 黑名单删除成功", plateNumber);
            } else {
                log.warn("车牌 {} 黑名单删除失败", plateNumber);
            }

            return success;
        } catch (Exception e) {
            log.error("根据车牌号删除黑名单失败，车牌: {}", plateNumber, e);
            return false;
        }
    }

    /**
     * 根据Oracle车辆信息添加黑名单
     * 仅当车辆标记为"需要停车检查"时才添加
     * 
     * @param vehicleInfo Oracle车辆信息
     * @return 是否成功（如果不需要添加黑名单也返回true）
     */
    public boolean addBlacklistFromOracle(com.parkingmanage.entity.OracleVehicleInfo vehicleInfo) {
        String plateNumber = vehicleInfo.getPlateNumber();
        
        // 检查是否需要添加黑名单
        if (!vehicleInfo.isNeedCheck()) {
            log.debug("车牌 {} 不需要停车检查，跳过黑名单添加", plateNumber);
            return true;
        }

        log.info("添加黑名单，车牌: {}, 原因: {}", plateNumber, vehicleInfo.getCheckReason());

        try {
            // 构建黑名单请求
            AddBlacklistCarRequest request = buildBlacklistRequest(vehicleInfo);

            // 调用添加黑名单接口
            boolean success = addBlacklistCar(request);
            
            if (success) {
                log.info("车牌 {} 添加黑名单成功，原因: {}", plateNumber, vehicleInfo.getCheckReason());
            } else {
                log.warn("车牌 {} 添加黑名单失败", plateNumber);
            }
            
            return success;
        } catch (Exception e) {
            log.error("从Oracle数据添加黑名单失败，车牌: {}", plateNumber, e);
            return false;
        }
    }

    /**
     * 根据Oracle车辆信息构建黑名单请求
     * 
     * @param vehicleInfo Oracle车辆信息
     * @return 黑名单请求
     */
    private AddBlacklistCarRequest buildBlacklistRequest(com.parkingmanage.entity.OracleVehicleInfo vehicleInfo) {
        AddBlacklistCarRequest request = new AddBlacklistCarRequest();
        
        request.setVipTypeCode(""); // 由系统自动分配
        request.setVipTypeName("请停车检查"); // 黑名单名称固定为"请停车检查"
        request.setCarCode(vehicleInfo.getPlateNumber());
        request.setCarOwner(vehicleInfo.getOwnerName());
        request.setReason("请停车检查"); // 原因固定为"请停车检查"
        
        // 根据有效期判断是临时还是永久黑名单
        if (vehicleInfo.getValidEndTime() != null) {
            // 有结束时间，设置为临时黑名单
            request.setIsPermament(0);
            
            AddBlacklistCarRequest.TimePeriod timePeriod = new AddBlacklistCarRequest.TimePeriod();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            if (vehicleInfo.getValidStartTime() != null) {
                timePeriod.setStartTime(vehicleInfo.getValidStartTime().format(formatter));
            } else {
                timePeriod.setStartTime(LocalDateTime.now().format(formatter));
            }
            timePeriod.setEndTime(vehicleInfo.getValidEndTime().format(formatter));
            
            request.setTimePeriod(timePeriod);
        } else {
            // 无结束时间，设置为永久黑名单
            request.setIsPermament(1);
        }
        
        request.setRemark1(vehicleInfo.getRemark());
        request.setRemark2("Oracle数据同步");
        request.setOperator(defaultOperator);
        request.setOperateTime(getCurrentTime());

        return request;
    }

    /**
     * 检查车辆是否需要添加黑名单
     * 
     * @param vehicleInfo Oracle车辆信息
     * @return true-需要添加黑名单，false-不需要
     */
    public boolean needAddBlacklist(com.parkingmanage.entity.OracleVehicleInfo vehicleInfo) {
        return vehicleInfo != null && vehicleInfo.isNeedCheck();
    }

    /**
     * 调用AKE接口
     */
    private String callAkeApi(String command, Map<String, Object> bizContent) {
        String url = baseUrl + API_PATH;

        Map<String, Object> request = new HashMap<>();
        request.put("command", command);
        request.put("message_id", String.valueOf(System.currentTimeMillis()));
        request.put("device_id", "");
        request.put("sign_type", "MD5");
        request.put("charset", "UTF-8");
        request.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        request.put("biz_content", bizContent);
        request.put("sign", appKey);

        String requestJson = JSON.toJSONString(request);
        log.debug("AKE请求: command={}, url={}", command, url);
        log.debug("AKE请求参数: {}", requestJson);

        String response = HttpClientUtil.doPostJson(url, requestJson);
        log.debug("AKE响应: {}", response);

        return response;
    }

    /**
     * 解析VIP票响应
     */
    private List<VipTicketInfo> parseVipTicketResponse(String response) {
        List<VipTicketInfo> result = new ArrayList<>();
        
        if (!StringUtils.hasText(response)) {
            return result;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");

            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                log.warn("查询VIP票失败: {}", bizContent != null ? bizContent.getString("msg") : "响应为空");
                return result;
            }

            com.alibaba.fastjson.JSONArray ticketList = bizContent.getJSONArray("ticket_list");
            if (ticketList == null || ticketList.isEmpty()) {
                return result;
            }

            for (int i = 0; i < ticketList.size(); i++) {
                JSONObject ticket = ticketList.getJSONObject(i);
                VipTicketInfo info = new VipTicketInfo();
                info.setTicketNo(ticket.getString("ticket_no"));
                info.setVipTicketSeq(ticket.getString("vip_ticket_seq"));
                info.setVipTypeName(ticket.getString("vip_type_name"));
                info.setCarOwner(ticket.getString("car_owner"));
                info.setTelphone(ticket.getString("telphone"));  // 解析车主电话
                info.setCarNo(ticket.getString("car_no"));
                info.setTicketStatus(ticket.getString("ticket_status"));
                
                // 尝试从time_period字段解析时间（格式：开始时间~结束时间）
                String timePeriod = ticket.getString("time_period");
                if (timePeriod != null && timePeriod.contains("~")) {
                    String[] times = timePeriod.split("~");
                    if (times.length == 2) {
                        info.setStartTime(times[0].trim());
                        info.setEndTime(times[1].trim());
                    }
                } else {
                    // 如果time_period不存在，尝试直接获取start_time和end_time
                    info.setStartTime(ticket.getString("start_time"));
                    info.setEndTime(ticket.getString("end_time"));
                }
                
                result.add(info);
            }

            log.info("查询到 {} 条VIP票记录", result.size());
        } catch (Exception e) {
            log.error("解析VIP票响应失败", e);
        }

        return result;
    }

    /**
     * 解析黑名单响应
     */
    private List<BlacklistInfo> parseBlacklistResponse(String response) {
        List<BlacklistInfo> result = new ArrayList<>();
        
        if (!StringUtils.hasText(response)) {
            return result;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");

            if (bizContent == null || !"0".equals(bizContent.getString("code"))) {
                log.warn("查询黑名单失败: {}", bizContent != null ? bizContent.getString("msg") : "响应为空");
                return result;
            }

            com.alibaba.fastjson.JSONArray blackList = bizContent.getJSONArray("black_list");
            if (blackList == null || blackList.isEmpty()) {
                return result;
            }

            for (int i = 0; i < blackList.size(); i++) {
                JSONObject black = blackList.getJSONObject(i);
                BlacklistInfo info = new BlacklistInfo();
                info.setBlacklistSeq(black.getString("blacklist_seq"));
                info.setCarLicenseNumber(black.getString("car_license_number"));
                info.setVipName(black.getString("vip_name"));
                info.setOwner(black.getString("owner"));
                info.setReason(black.getString("reason"));
                info.setTimeperiodList(black.getString("timeperiod_list"));
                info.setBlacklistForeverFlag(black.getString("blacklist_forever_flag"));
                info.setAddBy(black.getString("add_by"));
                info.setAddTime(black.getString("add_time"));
                info.setOperateBy(black.getString("operate_by"));
                info.setOperateTime(black.getString("operate_time"));
                result.add(info);
            }

            log.info("查询到 {} 条黑名单记录", result.size());
        } catch (Exception e) {
            log.error("解析黑名单响应失败", e);
        }

        return result;
    }

    /**
     * 解析成功响应
     */
    private boolean parseSuccessResponse(String response, String operation) {
        if (!StringUtils.hasText(response)) {
            log.warn("{}响应为空", operation);
            return false;
        }

        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONObject bizContent = jsonResponse.getJSONObject("biz_content");

            if (bizContent == null) {
                log.warn("{}响应缺少biz_content", operation);
                return false;
            }

            String code = bizContent.getString("code");
            String msg = bizContent.getString("msg");

            if ("0".equals(code)) {
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
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * VIP票信息
     */
    @Data
    public static class VipTicketInfo {
        private String ticketNo;
        private String vipTicketSeq;
        private String vipTypeName;
        private String carOwner;
        private String telphone;     // 车主电话
        private String carNo;
        private String ticketStatus;
        private String startTime;
        private String endTime;
    }

    /**
     * 黑名单信息
     */
    @Data
    public static class BlacklistInfo {
        private String blacklistSeq;           // 黑名单序列号
        private String carLicenseNumber;       // 车牌号
        private String vipName;                // 黑名单类型名称
        private String owner;                  // 车主
        private String reason;                 // 原因
        private String timeperiodList;         // 时间段列表（格式：开始时间~结束时间）
        private String blacklistForeverFlag;   // 永久标志：1-永久，0-临时
        private String addBy;                  // 添加人
        private String addTime;                // 添加时间
        private String operateBy;              // 操作人
        private String operateTime;            // 操作时间
    }
}
