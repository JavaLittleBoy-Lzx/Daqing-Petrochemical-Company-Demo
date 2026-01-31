package com.parkingmanage.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 分组后的车辆信息
 * 同一车牌号的多条记录合并为一个对象
 * 
 * 设计说明：
 * - 同一车牌可能有多条记录（不同CQDM/CQDMNAME）
 * - 需要合并同一车牌的所有权限（CQDMNAME）
 * - 时间信息（KYXQKSSJ、KYXQJSSJ）在同一车牌的多条记录中应该相同
 * - 其他基本信息（车主、单位等）也应该相同
 */
@Data
public class GroupedVehicleInfo {
    
    /** 车牌号码（分组键） */
    private String plateNumber;
    
    /** 卡号 */
    private String cardNo;
    
    /** 驾驶员姓名 */
    private String ownerName;
    
    /** 车主电话 */
    private String ownerPhone;
    
    /** 单位名称 */
    private String company;
    
    /** 车主部门 */
    private String department;
    
    /** 车辆类型名称 */
    private String vehicleType;
    
    /** 车辆类别名称 */
    private String vehicleCategory;
    
    /** 号牌颜色名称 */
    private String plateColor;
    
    /** 品牌型号 */
    private String brandModel;
    
    /** VIP类型名称 */
    private String vipTypeName;
    
    /** 有效期开始 */
    private LocalDateTime validStartTime;
    
    /** 有效期结束 */
    private LocalDateTime validEndTime;
    
    /** 是否需要停车检查（黑名单标记） */
    private boolean needCheck;
    
    /** 检查原因 */
    private String checkReason;
    
    /** 备注/当前状态名称 */
    private String remark;
    
    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 当前状态代码（DQZT）- D=注销，A=正常 */
    private String dqzt;

    /** 卡类型（KLX）- A=长期卡，D=临时卡 */
    private String klx;
    
    /** 
     * 厂区代码列表（同一车牌的多个CQDM）
     * 用于记录该车辆在哪些厂区有权限
     */
    private List<String> orgNos = new ArrayList<>();
    
    /** 
     * 厂区名称列表（同一车牌的多个CQDMNAME）
     * 这是核心字段，用于后续合并生成VIP类型名称或黑名单类型名称
     */
    private List<String> orgNames = new ArrayList<>();
    
    /** 
     * 原始记录列表
     * 保存所有原始记录，用于调试和追溯
     */
    private List<OracleVehicleInfo> originalRecords = new ArrayList<>();
    
    /**
     * 添加一条原始记录到分组中
     *
     * @param record 原始车辆记录
     */
    public void addRecord(OracleVehicleInfo record) {
        // 第一条记录：初始化基本信息
        if (originalRecords.isEmpty()) {
            this.plateNumber = record.getPlateNumber();
            this.cardNo = record.getCardNo();
            this.ownerName = record.getOwnerName();
            this.ownerPhone = record.getOwnerPhone();
            this.company = record.getCompany();
            this.department = record.getDepartment();
            this.vehicleType = record.getVehicleType();
            this.vehicleCategory = record.getVehicleCategory();
            this.plateColor = record.getPlateColor();
            this.brandModel = record.getBrandModel();
            this.vipTypeName = record.getVipTypeName();
            this.validStartTime = record.getValidStartTime();
            this.validEndTime = record.getValidEndTime();
            this.needCheck = record.isNeedCheck();
            this.checkReason = record.getCheckReason();
            this.createTime = record.getCreateTime();
            this.updateTime = record.getUpdateTime();
            this.dqzt = record.getDqzt();
            this.klx = record.getKlx();

            // 添加调试日志
            if (this.validStartTime == null || this.validEndTime == null) {
                System.out.println("⚠️ 车辆[" + this.plateNumber + "]时间字段为空 - " +
                        "开始时间: " + this.validStartTime + ", " +
                        "结束时间: " + this.validEndTime);
            }
        }
        // 每次都更新 remark（使用最新记录的 DQZT 状态）
        // 因为 Oracle 查询结果按 CZSJ 排序，最后添加的记录是最新的
        this.remark = record.getRemark();

        // 添加厂区信息（去重）
        if (record.getOrgNo() != null && !orgNos.contains(record.getOrgNo())) {
            orgNos.add(record.getOrgNo());
        }
        if (record.getOrgName() != null && !orgNames.contains(record.getOrgName())) {
            orgNames.add(record.getOrgName());
        }
        // 保存原始记录
        originalRecords.add(record);
    }
    
    /**
     * 获取记录数量
     * @return 该车牌的记录数量
     */
    public int getRecordCount() {
        return originalRecords.size();
    }
}
