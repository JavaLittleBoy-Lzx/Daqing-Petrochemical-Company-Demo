package com.parkingmanage.service.oracle;

import com.parkingmanage.entity.OraclePersonInfo;
import com.parkingmanage.entity.OracleVehicleInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Oracle数据服务
 * 负责从大庆石化Oracle数据库获取人员和车辆数据
 * 
 * 人员视图：pentranceguard.view_facedowninfo
 * 照片获取规则（根据rylx字段）：
 *   - rylx=1: SELECT photo_bf FROM docu.photo WHERE bxh=ryid
 *   - rylx=2或3: SELECT photo FROM pentranceguard.tcfacephoto WHERE sfzh=ryid
 *   - rylx=4或5: SELECT photo FROM pentranceguard.personfacepicinfo WHERE jlh=ryid
 */
@Slf4j
@Service
public class OracleDataService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取最新人员数据（根据操作时间CZSJ）
     * 从视图 pentranceguard.view_facedowninfo 查询
     * 
     * 实际字段映射（根据用户提供的表结构）：
     *   ID - 人脸ID
     *   RECORDNO - 记录号
     *   DWMCNAME - 单位名称
     *   RYLX - 人员类型编码
     *   RYLXNAME - 人员类型名称
     *   RYID - 人员ID
     *   XM - 姓名
     *   YXM - 曾用名
     *   XB - 性别代码
     *   XBNAME - 性别名称
     *   SFZH - 身份证号
     *   KH - 卡号
     *   CQDM - 厂区代码
     *   CQDMNAME - 厂区名称
     *   SBIP - 设备IP
     *   DQZT - 当前状态代码
     *   DQZTNAME - 当前状态名称
     *   KYXQKSSJ - 有效期开始时间
     *   KYXQJSSJ - 有效期结束时间
     *   CZSJ - 操作时间（用于增量同步）
     * 
     * @param lastSyncTime 上次同步时间
     * @return 人员信息列表
     */
    public List<OraclePersonInfo> getLatestPersonData(LocalDateTime lastSyncTime) {
        log.info("========== 开始查询Oracle人员数据 ==========");
        log.info("上次同步时间: {}", lastSyncTime);
        
        try {
            // 从视图 pentranceguard.view_facedowninfo 查询人员数据
            // 使用 CZSJ 字段进行增量查询（CZSJ是VARCHAR2类型，需要显式转换为TIMESTAMP进行比较）
            String sql = "SELECT ID, RECORDNO, DWMCNAME, RYLX, RYLXNAME, RYID, XM, XB, XBNAME, SFZH, " +
                        "KH, CQDM, CQDMNAME, DQZT, DQZTNAME, KYXQKSSJ, KYXQJSSJ, CZSJ " +
                        "FROM pentranceguard.view_facedowninfo " +
                        "WHERE CZSJ IS NOT NULL " +  // 过滤掉CZSJ为NULL的记录
                        "AND TO_TIMESTAMP(CZSJ, 'YYYY-MM-DD HH24:MI:SS') > TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS') " +
                        "ORDER BY CZSJ ASC";
            
            log.info("执行SQL查询（增量查询，CZSJ > {}）", lastSyncTime);
            
            // 格式化时间为字符串格式（匹配CZSJ字段的VARCHAR2格式）
            // 注意：使用标准格式 yyyy-MM-dd HH:mm:ss，不使用ISO格式（不带T）
            String timeStr = lastSyncTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("人员增量查询时间参数: [{}]", timeStr);
            
            List<OraclePersonInfo> persons = jdbcTemplate.query(sql, ps -> {
                ps.setString(1, timeStr);
            }, (rs, rowNum) -> {
                OraclePersonInfo person = new OraclePersonInfo();
                
                // 基本信息映射（使用实际字段名）
                String ryid = rs.getString("RYID");
                person.setEmployeeNo(ryid);
                person.setName(rs.getString("XM"));
                person.setIdCard(rs.getString("SFZH"));
                
                // 单位/部门信息
                person.setDepartment(rs.getString("DWMCNAME"));
                person.setOrgNo(rs.getString("CQDM")); // 厂区代码
                
                // 人员类型
                String rylx = rs.getString("RYLX");
                String rylxName = rs.getString("RYLXNAME");
                person.setPersonType(rylxName != null ? rylxName : convertPersonType(rylx));
                person.setRylx(rylx); // 保存原始rylx用于照片查询
                
                // 性别（使用XB字段）
                String xb = rs.getString("XB");
                String xbName = rs.getString("XBNAME");
                person.setSex(convertSex(xb, xbName));
                
                // 有效期（KYXQKSSJ, KYXQJSSJ 是VARCHAR2类型，需要字符串解析）
                String kyxqkssjStr = rs.getString("KYXQKSSJ");
                if (kyxqkssjStr != null && !kyxqkssjStr.trim().isEmpty()) {
                    person.setValidStartTime(parseDateTime(kyxqkssjStr));
                }
                String kyxqjssjStr = rs.getString("KYXQJSSJ");
                if (kyxqjssjStr != null && !kyxqjssjStr.trim().isEmpty()) {
                    person.setValidEndTime(parseDateTime(kyxqjssjStr));
                }
                
                // 当前状态
                String dqzt = rs.getString("DQZT");
                String dqztName = rs.getString("DQZTNAME");
                person.setDqzt(dqzt);
                person.setRemark(dqztName);

                // 门禁权限需要从其他地方获取或配置
                person.setGatePermissionStr(null);

                // log.debug("读取人员: RYID={}, XM={}, RYLX={}, 单位={}, 有效期={}~{}",
                //         ryid, person.getName(), rylx, person.getDepartment(),
                //         person.getValidStartTime(), person.getValidEndTime());

                return person;
            });
            
            log.info("查询到 {} 条人员数据（原始记录）", persons.size());
            
            // 按人员ID聚合：同一人员可能有多条记录，对应不同门禁权限（CQDM/CQDMNAME）
            Map<String, OraclePersonInfo> personMap = new LinkedHashMap<>();
            for (OraclePersonInfo person : persons) {
                String key = person.getEmployeeNo();
                if (!personMap.containsKey(key)) {
                    // 第一次遇到这个人员，直接添加
                    personMap.put(key, person);
                } else {
                    // 已存在该人员，合并门禁权限（CQDM）
                    OraclePersonInfo existing = personMap.get(key);
                    String newOrgNo = person.getOrgNo();
                    
                    // 将新的厂区代码添加到权限列表中（如果不重复）
                    if (newOrgNo != null && !newOrgNo.trim().isEmpty()) {
                        String existingPermissions = existing.getGatePermissionStr();
                        if (existingPermissions == null || existingPermissions.isEmpty()) {
                            existing.setGatePermissionStr(newOrgNo);
                        } else if (!existingPermissions.contains(newOrgNo)) {
                            existing.setGatePermissionStr(existingPermissions + "," + newOrgNo);
                        }
                    }

                    // log.debug("人员[{}]合并门禁权限: {}", key, existing.getGatePermissionStr());
                }
            }
            
            log.info("聚合后人员数: {} 人（原始记录: {} 条）", personMap.size(), persons.size());

            // 为每个人员加载照片，只返回有照片的人员
            // 若人员在照片表中没有找到照片，则不同步该人员
            List<OraclePersonInfo> personsWithPhoto = new ArrayList<>();
            int noPhotoCount = 0;
            for (OraclePersonInfo person : personMap.values()) {
                String photoBase64 = getPersonPhoto(person.getEmployeeNo(), person.getRylx());
                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    person.setPhotoBase64(photoBase64);
                    personsWithPhoto.add(person);
                    log.debug("人员[{}]照片加载成功，Base64长度: {}, 门禁权限: {}",
                            person.getEmployeeNo(), photoBase64.length(), person.getGatePermissionStr());
                } else {
                    noPhotoCount++;
                    log.debug("人员[{}]无照片，不同步", person.getEmployeeNo());
                }
            }

            log.info("照片加载完成，有照片: {} 人，无照片: {} 人（已过滤）", personsWithPhoto.size(), noPhotoCount);
            log.info("========== Oracle人员数据查询完成 ==========");

            // 只返回有照片的人员
            return personsWithPhoto;
            
        } catch (DataAccessException e) {
            log.error("查询Oracle人员数据失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }


    /**
     * 获取人员照片（BLOB转Base64）
     * 根据rylx字段从不同表获取照片：
     *   - rylx=1: SELECT photo_bf FROM docu.photo WHERE bxh=ryid
     *   - rylx=2或3: SELECT photo FROM pentranceguard.tcfacephoto WHERE sfzh=ryid
     *   - rylx=4或5: SELECT photo FROM pentranceguard.personfacepicinfo WHERE jlh=ryid
     * 
     * @param ryid 人员ID
     * @param rylx 人员类型
     * @return Base64编码的照片字符串，如果不存在则返回null
     */
    public String getPersonPhoto(String ryid, String rylx) {
        if (ryid == null || ryid.trim().isEmpty()) {
            log.warn("人员ID为空，无法查询照片");
            return null;
        }
        
        if (rylx == null || rylx.trim().isEmpty()) {
            log.warn("人员类型为空，无法确定照片查询表，ryid={}", ryid);
            return null;
        }
        
        String sql;
        String photoColumn;
        String tableName;
        String whereColumn;
        
        // 根据rylx确定查询的表和字段
        // 表结构：
        //   docu.photo: BXH(保险号), PHOTO_BF(照片), EDIT_DATETIME(编辑时间)
        //   pentranceguard.tcfacephoto: SFZH(身份证号), PHOTO(照片), EDIT_DATETIME(编辑时间)
        //   pentranceguard.personfacepicinfo: JLH(记录号), PHOTO(照片), EDIT_DATETIME(编辑时间)
        switch (rylx.trim()) {
            case "1":
                // rylx=1: 从 docu.photo 表查询正式职工照片
                sql = "SELECT PHOTO_BF FROM docu.photo WHERE BXH = ?";
                photoColumn = "PHOTO_BF";
                tableName = "docu.photo";
                whereColumn = "BXH";
                break;
            case "2":
            case "3":
                // rylx=2或3: 从 pentranceguard.tcfacephoto 表查询劳务用工照片
                sql = "SELECT PHOTO FROM pentranceguard.tcfacephoto WHERE SFZH = ?";
                photoColumn = "PHOTO";
                tableName = "pentranceguard.tcfacephoto";
                whereColumn = "SFZH";
                break;
            case "4":
            case "5":
                // rylx=4或5: 从 pentranceguard.personfacepicinfo 表查询施工人员照片
                sql = "SELECT PHOTO FROM pentranceguard.personfacepicinfo WHERE JLH = ?";
                photoColumn = "PHOTO";
                tableName = "pentranceguard.personfacepicinfo";
                whereColumn = "JLH";
                break;
            default:
                log.warn("未知的人员类型rylx={}，无法确定照片查询表，ryid={}", rylx, ryid);
                return null;
        }
        
        log.debug("查询照片: ryid={}, rylx={}, 表={}, 条件={}={}", ryid, rylx, tableName, whereColumn, ryid);
        
        try {
            return jdbcTemplate.query(sql, rs -> {
                if (rs.next()) {
                    Blob photoBlob = rs.getBlob(photoColumn);
                    String base64 = blobToBase64(photoBlob);
                    if (base64 != null) {
                        log.debug("照片查询成功: ryid={}, 表={}, Base64长度={}", ryid, tableName, base64.length());
                    }
                    return base64;
                }
                log.debug("照片不存在: ryid={}, 表={}", ryid, tableName);
                return null;
            }, ryid);
        } catch (DataAccessException e) {
            log.error("查询人员照片失败: ryid={}, rylx={}, 表={}, 错误: {}", ryid, rylx, tableName, e.getMessage());
            return null;
        }
    }

    /**
     * 获取照片有更新的人员ID列表（增量同步）
     * 根据照片表的EDIT_DATETIME字段判断照片是否有更新
     * 
     * @param lastSyncTime 上次同步时间
     * @return 照片有更新的人员ID和类型列表
     */
    public List<PhotoUpdateInfo> getUpdatedPhotoPersonIds(LocalDateTime lastSyncTime) {
        log.info("========== 开始查询照片更新 ==========");
        log.info("上次同步时间: {}", lastSyncTime);
        
        List<PhotoUpdateInfo> updatedList = new ArrayList<>();
        // 格式化时间为字符串格式（EDIT_DATETIME可能也是VARCHAR2类型，直接字符串比较）
        String timeStr = lastSyncTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        // 1. 查询正式职工照片更新 (rylx=1, docu.photo表)
        // BXH就是RYID，EDIT_DATETIME是VARCHAR2类型需要显式转换
        try {
            String sql1 = "SELECT BXH FROM docu.photo WHERE TO_TIMESTAMP(EDIT_DATETIME, 'YYYY-MM-DD HH24:MI:SS') > TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')";
            List<String> type1Updates = jdbcTemplate.queryForList(sql1, String.class, timeStr);
            for (String bxh : type1Updates) {
                updatedList.add(new PhotoUpdateInfo(bxh, "1"));
            }
            log.info("正式职工照片更新: {} 条", type1Updates.size());
        } catch (DataAccessException e) {
            log.warn("查询正式职工照片更新失败: {}", e.getMessage());
        }
        
        // 2. 查询劳务用工照片更新 (rylx=2,3, pentranceguard.tcfacephoto表)
        // SFZH就是RYID，EDIT_DATETIME是VARCHAR2类型需要显式转换
        try {
            String sql2 = "SELECT SFZH FROM pentranceguard.tcfacephoto WHERE TO_TIMESTAMP(EDIT_DATETIME, 'YYYY-MM-DD HH24:MI:SS') > TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')";
            List<String> type2Updates = jdbcTemplate.queryForList(sql2, String.class, timeStr);
            for (String sfzh : type2Updates) {
                updatedList.add(new PhotoUpdateInfo(sfzh, "2")); // SFZH就是RYID
            }
            log.info("劳务用工照片更新: {} 条", type2Updates.size());
        } catch (DataAccessException e) {
            log.warn("查询劳务用工照片更新失败: {}", e.getMessage());
        }
        
        // 3. 查询施工人员照片更新 (rylx=4,5, pentranceguard.personfacepicinfo表)
        // JLH就是RYID，EDIT_DATETIME是VARCHAR2类型需要显式转换
        try {
            String sql3 = "SELECT JLH FROM pentranceguard.personfacepicinfo WHERE TO_TIMESTAMP(EDIT_DATETIME, 'YYYY-MM-DD HH24:MI:SS') > TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS')";
            List<String> type3Updates = jdbcTemplate.queryForList(sql3, String.class, timeStr);
            for (String jlh : type3Updates) {
                updatedList.add(new PhotoUpdateInfo(jlh, "4")); // JLH就是RYID
            }
            log.info("施工人员照片更新: {} 条", type3Updates.size());
        } catch (DataAccessException e) {
            log.warn("查询施工人员照片更新失败: {}", e.getMessage());
        }
        
        log.info("========== 照片更新查询完成，共 {} 条 ==========", updatedList.size());
        return updatedList;
    }

    /**
     * 照片更新信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class PhotoUpdateInfo {
        /** 人员ID（BXH/SFZH/JLH） */
        private String personId;
        /** 人员类型 */
        private String rylx;
    }

    /**
     * BLOB转Base64
     *
     * @param blob Oracle BLOB对象
     * @return Base64编码字符串，如果BLOB为空或转换失败则返回null
     */
    private String blobToBase64(Blob blob) {
        if (blob == null) {
            return null;
        }
        try {
            long blobLength = blob.length();
            // 创建一个字节数组，长度为BLOB的长度
            if (blobLength == 0) {
                log.debug("BLOB内容为空");
                return null;
            }
            byte[] bytes = blob.getBytes(1, (int) blobLength);

            // 输出BLOB原始数据的前100字节和后100字节（十六进制）
            String prefix = bytesToHex(bytes, 0, Math.min(100, bytes.length));
            String suffix = bytesToHex(bytes, Math.max(0, bytes.length - 100), Math.min(100, bytes.length));
            // log.debug("BLOB原始数据 - 大小: {} bytes, 前100字节(hex): {}, 后100字节(hex): {}",
            //         blobLength, prefix, suffix);

            String base64 = Base64.getEncoder().encodeToString(bytes);

            // 输出Base64编码后的前500字符和后500字符
            String base64Prefix = base64.substring(0, Math.min(500, base64.length()));
            String base64Suffix = base64.length() > 500
                    ? base64.substring(Math.max(0, base64.length() - 500))
                    : "";
            // log.debug("BLOB转Base64成功 - 原始大小: {} bytes, Base64长度: {}", blobLength, base64.length());
            // log.debug("Base64数据 - 前500字符: {}, 后500字符: {}", base64Prefix, base64Suffix);

            return base64;
        } catch (SQLException e) {
            log.error("BLOB转Base64失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 字节数组转十六进制字符串
     *
     * @param bytes 字节数组
     * @param offset 起始位置
     * @param length 长度
     * @return 十六进制字符串
     */
    private String bytesToHex(byte[] bytes, int offset, int length) {
        if (bytes == null || offset < 0 || offset >= bytes.length) {
            return "";
        }
        int end = Math.min(offset + length, bytes.length);
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < end; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }



    /**
     * 转换人员类型
     * 将Oracle中的rylx转换为业务人员类型
     * 
     * @param rylx Oracle人员类型代码
     * @return 业务人员类型名称
     */
    private String convertPersonType(String rylx) {
        if (rylx == null) {
            return "未知";
        }
        switch (rylx.trim()) {
            case "1":
                return "正式员工";
            case "2":
                return "子女工";
            case "3":
                return "外来员工";
            case "4":
            case "5":
                return "施工人员";
            default:
                log.debug("未知人员类型代码: {}", rylx);
                return "其他";
        }
    }

    /**
     * 转换性别
     * 
     * @param xb Oracle性别代码
     * @param xbName Oracle性别名称
     * @return 性别：0-未知 1-男 2-女
     */
    private Integer convertSex(String xb, String xbName) {
        // 优先使用性别名称
        if (xbName != null && !xbName.trim().isEmpty()) {
            String name = xbName.trim();
            if (name.contains("男") || name.equalsIgnoreCase("male")) {
                return 1;
            } else if (name.contains("女") || name.equalsIgnoreCase("female")) {
                return 2;
            }
        }
        
        // 使用性别代码
        if (xb != null && !xb.trim().isEmpty()) {
            switch (xb.trim()) {
                case "1":
                case "M":
                case "男":
                    return 1;
                case "2":
                case "F":
                case "女":
                    return 2;
            }
        }
        
        return 0; // 未知
    }


    /**
     * 获取最新车辆数据
     * 从视图 aentranceguard.view_autovalidinfo 查询
     * 
     * 实际字段映射（根据用户提供的表结构）：
     *   RECORDNOL - 记录号L
     *   RECORDNO - 记录号
     *   KH - 卡号
     *   CPHM - 车牌号码
     *   HPYS - 号牌颜色代码
     *   HPYSNAME - 号牌颜色名称
     *   CLZL - 车辆种类代码
     *   CLZLNAME - 车辆种类名称
     *   CLLX - 车辆类型代码
     *   CLLXNAME - 车辆类型名称
     *   PPXH - 品牌型号
     *   DWMCNAME - 单位名称
     *   JSYXM - 驾驶员姓名
     *   CODE - 代码
     *   CQDM - 厂区代码
     *   CQDMNAME - 厂区名称
     *   KYXQKSSJ - 有效期开始时间
     *   KYXQJSSJ - 有效期结束时间
     *   DQZT - 当前状态代码
     *   DQZTNAME - 当前状态名称
     *   ISCHECK - 是否检查
     *   ISCHECKNAME - 检查名称
     *   CZSJ - 操作时间（用于增量同步）
     * 
     * @param lastSyncTime 上次同步时间
     * @return 车辆信息列表
     */
    public List<OracleVehicleInfo> getLatestVehicleData(LocalDateTime lastSyncTime) {
        log.info("========== 开始查询Oracle车辆数据 ==========");
        log.info("上次同步时间: {}", lastSyncTime);
        
        try {
            // 从视图 aentranceguard.view_autovalidinfo 查询车辆数据（注意是aentranceguard不是pentranceguard）
            // 使用 CZSJ 字段进行增量查询（CZSJ是VARCHAR2类型，需要显式转换为TIMESTAMP进行比较）
            // 查询所有门的车辆数据，不限制DQZT和CQDMNAME
            String sql = "SELECT KH, CPHM, HPYS, HPYSNAME, CLZL, CLZLNAME, CLLX, CLLXNAME, " +
                        "PPXH, DWMCNAME, JSYXM, CQDM, CQDMNAME, KLX, " +  // 添加KLX字段
                        "KYXQKSSJ, KYXQJSSJ, DQZT, DQZTNAME, ISCHECK, ISCHECKNAME, CZSJ " +
                        "FROM aentranceguard.view_autovalidinfo " +
                        "WHERE CZSJ IS NOT NULL " +  // 过滤掉CZSJ为NULL的记录
                        "AND TO_TIMESTAMP(CZSJ, 'YYYY-MM-DD HH24:MI:SS') > TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS') " +
                        "ORDER BY CZSJ ASC";
            
            log.info("执行SQL查询（增量查询，CZSJ > {}）", lastSyncTime);
            
            // 格式化时间为字符串格式（匹配CZSJ字段的VARCHAR2格式）
            // 注意：使用标准格式 yyyy-MM-dd HH:mm:ss，不使用ISO格式（不带T）
            String timeStr = lastSyncTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("车辆增量查询时间参数: [{}]", timeStr);
            
            List<OracleVehicleInfo> vehicles = jdbcTemplate.query(sql, ps -> {
                ps.setString(1, timeStr);
            }, (rs, rowNum) -> {
                OracleVehicleInfo vehicle = new OracleVehicleInfo();
                
                // 基本信息映射（使用实际字段名）
                vehicle.setPlateNumber(rs.getString("CPHM"));
                vehicle.setOwnerName(rs.getString("JSYXM"));  // 驾驶员姓名
                vehicle.setCardNo(rs.getString("KH"));        // 卡号
                vehicle.setCompany(rs.getString("DWMCNAME")); // 单位名称
                vehicle.setOrgNo(rs.getString("CQDM"));       // 厂区代码
                vehicle.setOrgName(rs.getString("CQDMNAME")); // 厂区名称
                
                // 车辆类型信息
                vehicle.setVehicleType(rs.getString("CLLXNAME"));     // 车辆类型名称
                vehicle.setVehicleCategory(rs.getString("CLZLNAME")); // 车辆种类名称
                vehicle.setPlateColor(rs.getString("HPYSNAME"));      // 号牌颜色名称
                vehicle.setBrandModel(rs.getString("PPXH"));          // 品牌型号
                
                // 有效期（KYXQKSSJ, KYXQJSSJ 是VARCHAR2类型，需要字符串解析）
                String kyxqkssjStr = rs.getString("KYXQKSSJ");
                if (kyxqkssjStr != null && !kyxqkssjStr.trim().isEmpty()) {
                    vehicle.setValidStartTime(parseDateTime(kyxqkssjStr));
                }
                String kyxqjssjStr = rs.getString("KYXQJSSJ");
                if (kyxqjssjStr != null && !kyxqjssjStr.trim().isEmpty()) {
                    vehicle.setValidEndTime(parseDateTime(kyxqjssjStr));
                }
                
                // 当前状态
                String dqzt = rs.getString("DQZT");
                String dqztName = rs.getString("DQZTNAME");
                vehicle.setDqzt(dqzt);
                vehicle.setRemark(dqztName);

                // 卡类型
                String klx = rs.getString("KLX");
                vehicle.setKlx(klx);

                // 是否需要检查（黑名单标记）
                String isCheck = rs.getString("ISCHECK");
                String isCheckName = rs.getString("ISCHECKNAME");
                vehicle.setNeedCheck("1".equals(isCheck) || "是".equals(isCheckName));
                vehicle.setCheckReason(isCheckName);
                
                log.debug("读取车辆: CPHM={}, JSYXM={}, 单位={}, 厂区={}, 状态={}, 有效期={}~{}, 需要检查={}", 
                        vehicle.getPlateNumber(), vehicle.getOwnerName(), 
                        vehicle.getCompany(), vehicle.getOrgName(), dqztName,
                        vehicle.getValidStartTime(), vehicle.getValidEndTime(), 
                        vehicle.isNeedCheck());
                
                return vehicle;
            });
            
            log.info("查询到 {} 条车辆数据", vehicles.size());
            log.info("========== Oracle车辆数据查询完成 ==========");
            
            return vehicles;
            
        } catch (DataAccessException e) {
            log.error("查询Oracle车辆数据失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 测试Oracle数据库连接
     * 
     * @return true表示连接成功
     */
    public boolean testConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1 FROM DUAL", Integer.class);
            log.info("Oracle数据库连接测试成功");
            return true;
        } catch (DataAccessException e) {
            log.error("Oracle数据库连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取人员视图的总记录数
     * 用于验证视图是否可访问
     * 
     * @return 记录数，如果查询失败返回-1
     */
    public long getPersonViewCount() {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pentranceguard.view_facedowninfo", Long.class);
            log.info("人员视图总记录数: {}", count);
            return count != null ? count : 0;
        } catch (DataAccessException e) {
            log.error("查询人员视图记录数失败: {}", e.getMessage());
            return -1;
        }
    }

    /**
     * 解析VARCHAR2格式的日期时间字符串为LocalDateTime
     * 支持多种格式：
     *   - yyyy-MM-dd HH:mm:ss
     *   - yyyy/MM/dd HH:mm:ss
     *   - yyyyMMddHHmmss
     *   - yyyy-MM-dd
     * 
     * @param dateTimeStr 日期时间字符串
     * @return LocalDateTime对象，解析失败返回null
     */
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        String str = dateTimeStr.trim();
        
        // 尝试多种格式解析
        java.time.format.DateTimeFormatter[] formatters = {
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
        };
        
        for (java.time.format.DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(str, formatter);
            } catch (Exception ignored) {
                // 尝试下一个格式
            }
        }
        
        // 尝试只有日期的格式
        java.time.format.DateTimeFormatter[] dateOnlyFormatters = {
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")
        };
        
        for (java.time.format.DateTimeFormatter formatter : dateOnlyFormatters) {
            try {
                java.time.LocalDate date = java.time.LocalDate.parse(str, formatter);
                return date.atStartOfDay();
            } catch (Exception ignored) {
                // 尝试下一个格式
            }
        }
        
        log.warn("无法解析日期时间字符串: {}", dateTimeStr);
        return null;
    }
}
