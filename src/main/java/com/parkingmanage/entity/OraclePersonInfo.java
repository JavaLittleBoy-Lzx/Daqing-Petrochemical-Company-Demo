package com.parkingmanage.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Oracle人员信息实体
 * 从Oracle数据库视图 pentranceguard.view_facedowninfo 获取的人员原始数据
 * 
 * 实际字段映射（根据用户提供的表结构）：
 *   ID -> 人脸ID（未使用）
 *   RECORDNO -> 记录号（未使用）
 *   dwmcname -> department (单位名称)
 *   RYLX -> rylx (人员类型编码)
 *   RYLXNAME -> personType (人员类型名称)
 *   RYID -> employeeNo (人员ID/员工编号)
 *   XM -> name (姓名)
 *   YXB -> sex (性别)
 *   SFZH -> idCard (身份证号)
 *   KH -> 卡号（未使用）
 *   cqdm -> orgNo (厂区代码/组织编号)
 *   cqdmname -> 厂区名称（未使用）
 *   DQZT -> 当前状态编码
 *   DQZTNAME -> remark (当前状态名称)
 *   RYXQKSSJ -> validStartTime (有效期开始时间)
 *   RYXQJSSJ -> validEndTime (有效期结束时间)
 *   DMBM -> gatePermissionStr (大门编码，对应威尔接口的doorId)
 *   DMMC -> gateName (大门名称)
 * 
 * 照片获取规则（根据rylx字段从不同表获取）：
 *   - rylx=1: SELECT photo_bf FROM docu.photo WHERE bxh=ryid
 *   - rylx=2或3: SELECT photo FROM pentranceguard.tcfacephoto WHERE sfzh=ryid
 *   - rylx=4或5: SELECT photo FROM pentranceguard.personfacepicinfo WHERE jlh=ryid
 */
@Data
public class OraclePersonInfo {
    
    /** 员工编号/人员ID（唯一约束，对应RYID） */
    private String employeeNo;
    
    /** 姓名（对应XM） */
    private String name;
    
    /** 身份证号（对应SFZH） */
    private String idCard;
    
    /** 电话（视图中可能没有此字段） */
    private String phone;
    
    /** 部门/单位名称（对应dwmcname） */
    private String department;
    
    /** 组织编号/厂区代码（对应cqdm） */
    private String orgNo;
    
    /** 人员类型名称：正式员工/子女工/外来员工/施工人员（对应RYLXNAME） */
    private String personType;
    
    /** 原始人员类型代码（对应RYLX，用于照片查询和规则判断） */
    private String rylx;
    
    /** 性别：0-未知 1-男 2-女（转换自YXB） */
    private Integer sex;
    
    /** 照片（BLOB转Base64，根据rylx从不同表获取） */
    private String photoBase64;
    
    /** 有效期开始（对应RYXQKSSJ） */
    private LocalDateTime validStartTime;
    
    /** 有效期结束（对应RYXQJSSJ） */
    private LocalDateTime validEndTime;
    
    /** 门禁权限/大门编码（对应DMBM，直接作为威尔接口的doorId） */
    private String gatePermissionStr;
    
    /** 大门名称（对应DMMC） */
    private String gateName;
    
    /** 门禁权限列表（解析自gatePermissionStr） */
    private List<Integer> gatePermissions;
    
    /** 创建时间（视图中可能没有此字段） */
    private LocalDateTime createTime;

    /** 更新时间（视图中可能没有此字段） */
    private LocalDateTime updateTime;

    /** 当前状态代码（对应DQZT）- D=注销，A=正常 */
    private String dqzt;

    /** 备注/当前状态名称（对应DQZTNAME） */
    private String remark;
}
